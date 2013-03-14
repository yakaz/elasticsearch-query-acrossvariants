package org.apache.lucene.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Take each token from the analyzer,
 * build an OR query for the token to be present in any field
 * AND it all together so that each token must match something.
 * For tokens at the same position, analyze the offsets.
 * We assume there is only "included into" (parent) and
 * "next to" (sibling) relationships, ie. that there are
 * no "partial hover" between them.
 * Take the largest spans (most top level), AND them together.
 * Take each subspan as alternatives for the parent span,
 * AND the subspans together, and OR them with the parent span.
 */
public class AcrossVariantsAndQuery extends Query {

    public static interface QueryProvider {
        public Query queryTerm(String field, String term);
    }

    protected final TreeVisitor TREE_VISITOR = new TreeVisitor();

    private final Map<String, Float> boostedFields;
    private final Analyzer searchAnalyzer;
    private final String text;
    private final QueryProvider queryProvider;
    private float boost;
    protected TermNode termTree;

    public AcrossVariantsAndQuery(Collection<String> fields, Analyzer searchAnalyzer, String text) throws IOException {
        this(mapizeFields(fields), searchAnalyzer, text);
    }

    public AcrossVariantsAndQuery(Map<String, Float> boostedFields, Analyzer searchAnalyzer, String text) throws IOException {
        this(boostedFields, searchAnalyzer, text, TermQueryProvider.INSTANCE);
    }

    public AcrossVariantsAndQuery(Collection<String> fields, Analyzer searchAnalyzer, String text, QueryProvider queryProvider) throws IOException {
        this(mapizeFields(fields), searchAnalyzer, text, queryProvider);
    }

    public AcrossVariantsAndQuery(Map<String, Float> boostedFields, Analyzer searchAnalyzer, String text, QueryProvider queryProvider) throws IOException {
        this.boostedFields = boostedFields;
        this.searchAnalyzer = searchAnalyzer;
        this.text = text;
        this.queryProvider = queryProvider;
        this.boost = 1.0f;
        this.termTree = buildTree(new StringReader(text));
    }

    private static Map<String, Float> mapizeFields(Collection<String> fields) {
        Float defaultBoost = 1.0f;
        Map<String, Float> rtn = new HashMap<String, Float>(fields.size());
        for (String field : fields)
            rtn.put(field, defaultBoost);
        return rtn;
    }

    protected TermNode buildTree(Reader input) throws IOException {
        TermNode root = new TermNode(null);

        // Logic similar to QueryParser#getFieldQuery
        final TokenStream source = searchAnalyzer.tokenStream(null, input);
        source.reset();

        final CharTermAttribute termAtt = source.addAttribute(CharTermAttribute.class);
        final OffsetAttribute offsetAtt = source.addAttribute(OffsetAttribute.class);
        final PositionIncrementAttribute posIncrAtt = source.addAttribute(PositionIncrementAttribute.class);
        int pos = 0;
        List<PositionedTerm> collectedTokens = new ArrayList<PositionedTerm>();
        while (source.incrementToken()) {
            pos += posIncrAtt.getPositionIncrement();
            collectedTokens.add(new PositionedTerm(termAtt.toString(), offsetAtt.startOffset(), offsetAtt.endOffset(), pos));
        }
        source.end();
        source.reset();

        Collections.sort(collectedTokens);
        for (PositionedTerm term : collectedTokens)
            root.add(term);

        return root;
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query rtn = termTree.visit(TREE_VISITOR);
        rtn.setBoost(boost);
        return rtn.rewrite(reader);
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    public Set<String> getFields() {
        return boostedFields.keySet();
    }

    public Map<String, Float> getBoostedFields() {
        return boostedFields;
    }

    public Analyzer getSearchAnalyzer() {
        return searchAnalyzer;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Map.Entry<String, Float> boostedField : boostedFields.entrySet()) {
            if (first) first = false;
            else sb.append(',');
            sb.append(boostedField.getKey());
            if (boostedField.getValue() != 1.0f) {
                sb.append('^');
                sb.append(boostedField.getValue());
            }
        }
        sb.append("]:\"");
        sb.append(text);
        sb.append("\"");
        if (boost != 1.0f) {
            sb.append('^');
            sb.append(boost);
        }
        return sb.toString();
    }

    public static class TermQueryProvider implements QueryProvider {

        public static final TermQueryProvider INSTANCE = new TermQueryProvider();

        @Override
        public Query queryTerm(String field, String term) {
            return new TermQuery(new Term(field, term));
        }

    }

    protected static class PositionedTerm implements Comparable<PositionedTerm> {

        public final String term;
        public final int startOffset;
        public final int endOffset;
        public final int position;

        public PositionedTerm(String term, int startOffset, int endOffset, int position) {
            this.term = term;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.position = position;
        }

        @Override
        public String toString() {
            if (term == null)
                return "null";
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            sb.append(term);
            sb.append("\":[");
            sb.append(position);
            sb.append(',');
            sb.append(startOffset);
            sb.append(':');
            sb.append(endOffset);
            sb.append(']');
            return sb.toString();
        }

        @Override
        public int compareTo(PositionedTerm o) {
            if (o == null)
                return 1;
            int diff = this.position - o.position;
            if (diff == 0)
                diff = this.startOffset - o.startOffset;
            if (diff == 0)
                diff = o.endOffset - this.endOffset;
            return diff;
        }

        public boolean contains(PositionedTerm o) {
            return this.position == o.position
                    && this.startOffset <= o.startOffset
                    && this.endOffset >= o.endOffset;
        }

    }

    protected static class TermNode implements Comparable<Object> {

        protected final PositionedTerm term;
        protected List<String> alternateWritings;
        protected List<TermNode> children;

        public TermNode(PositionedTerm term) {
            this.term = term;
            this.alternateWritings = null;
            this.children = null;
        }

        public TermNode add(PositionedTerm term) {
            if (children == null) {
                TermNode newNode = new TermNode(term);
                addChild(newNode);
                return newNode;
            } else {
                int index = Collections.binarySearch(children, term);
                if (index >= 0) {
                    // Exact match (but for the term text)
                    // The two terms nest inside each other
                    // Add the new term as an alternate writing
                    TermNode node = children.get(index);
                    if (node.alternateWritings == null)
                        node.alternateWritings = new ArrayList<String>();
                    node.alternateWritings.add(term.term);
                    return node;
                } else {
                    index = -index - 1;
                    if (index > 0) {
                        // New term may be nestable in the previous element,
                        // or it can be the other way around
                        TermNode prev = children.get(index - 1);
                        if (prev.term.contains(term)) {
                            return prev.add(term);
                        }
                    }
                    TermNode newNode = new TermNode(term);
                    children.add(index, newNode);
                    return newNode;
                }
            }
        }

        public void addChild(TermNode child) {
            if (children == null)
                children = new ArrayList<TermNode>();
            children.add(child);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('<');
            if (alternateWritings != null) {
                for (String alternateWriting : alternateWritings) {
                    sb.append('"');
                    sb.append(alternateWriting);
                    sb.append("\"|");
                }
            }
            sb.append(term);
            sb.append('>');
            if (children == null)
                sb.append("[]");
            else
                sb.append(children);
            return sb.toString();
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof TermNode)
                return compareTo((TermNode)o);
            if (o instanceof PositionedTerm)
                return compareTo((PositionedTerm)o);
            throw new IllegalArgumentException("Can only compare to TermNode or PositionedTerm");
        }

        public int compareTo(TermNode o) {
            return this.compareTo(o.term);
        }

        public int compareTo(PositionedTerm o) {
            if (this.term == null)
                return 1;
            return this.term.compareTo(o);
        }

        public <T> T visit(Visitor<T> visitor) {
            List<T> childrenOutput = null;
            if (children != null && !children.isEmpty()) {
                childrenOutput = new ArrayList<T>(children.size());
                for (TermNode child : children)
                    childrenOutput.add(child.visit(visitor));
            }
            return visitor.visit(this, childrenOutput);
        }

        protected static interface Visitor<T> {
            public T visit(TermNode node, List<T> childrenOutput);
        }

    }

    protected class TreeVisitor implements TermNode.Visitor<Query> {

        @Override
        public Query visit(TermNode node, List<Query> childrenOutput) {
            Query subQueries = null;
            if (childrenOutput != null && !childrenOutput.isEmpty()) {
                if (childrenOutput.size() == 1) {
                    subQueries = childrenOutput.get(0);
                } else {
                    BooleanQuery _subQueries = new BooleanQuery(true);
                    for (Query subquery : childrenOutput)
                        _subQueries.add(subquery, BooleanClause.Occur.MUST);
                    subQueries = _subQueries;
                }
            }

            if (node.term == null) {

                // Root node
                if (subQueries == null)
                    return new BooleanQuery(true);
                return subQueries;

            } else {

                BooleanQuery nodeQuery = new BooleanQuery(true);
                for (Map.Entry<String, Float> boostedField : boostedFields.entrySet()) {
                    String field = boostedField.getKey();
                    float boost = boostedField.getValue();
                    Query query = queryProvider.queryTerm(field, node.term.term);
                    query.setBoost(boost);
                    nodeQuery.add(query, BooleanClause.Occur.SHOULD);
                    if (node.alternateWritings != null) {
                        for (String alternateWriting : node.alternateWritings) {
                            query = queryProvider.queryTerm(field, alternateWriting);
                            query.setBoost(boost);
                            nodeQuery.add(query, BooleanClause.Occur.SHOULD);
                        }
                    }
                }

                if (subQueries == null)
                    return nodeQuery;

                BooleanQuery rtn = new BooleanQuery(true);
                rtn.add(nodeQuery, BooleanClause.Occur.SHOULD);
                rtn.add(subQueries, BooleanClause.Occur.SHOULD);
                return rtn;

            }
        }

    }

}
