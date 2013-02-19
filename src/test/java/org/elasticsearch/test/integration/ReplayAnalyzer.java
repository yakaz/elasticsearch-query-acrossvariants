package org.elasticsearch.test.integration;

import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;

import java.io.Reader;

public class ReplayAnalyzer extends ReusableAnalyzerBase {

    protected final String[] tokens;
    protected final int[] positionIncrements;
    protected final int[] startOffsets;
    protected final int[] endOffsets;

    public ReplayAnalyzer(String[] tokens, int[] positionIncrements, int[] startOffsets, int[] endOffsets) {
        this.tokens = tokens;
        this.positionIncrements = positionIncrements;
        this.startOffsets = startOffsets;
        this.endOffsets = endOffsets;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
        return new TokenStreamComponents(
                new KeywordTokenizer(aReader), // dummy tokenizer, actually unused
                new ReplayTokenStream(tokens, positionIncrements, startOffsets, endOffsets));
    }

}
