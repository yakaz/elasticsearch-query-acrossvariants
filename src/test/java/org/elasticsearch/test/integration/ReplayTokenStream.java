package org.elasticsearch.test.integration;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;

public class ReplayTokenStream extends TokenStream {

    protected final CharTermAttribute termAttr;
    protected final PositionIncrementAttribute posIncrAttr;
    protected final OffsetAttribute offsetAttr;

    protected int pos;
    protected final String[] tokens;
    protected final int[] positionIncrements;
    protected final int[] startOffsets;
    protected final int[] endOffsets;

    public ReplayTokenStream(String[] tokens, int[] positionIncrements, int[] startOffsets, int[] endOffsets) {
        this.pos = 0;
        this.tokens = tokens;
        this.positionIncrements = positionIncrements;
        this.startOffsets = startOffsets;
        this.endOffsets = endOffsets;
        this.termAttr = addAttribute(CharTermAttribute.class);
        this.posIncrAttr = addAttribute(PositionIncrementAttribute.class);
        this.offsetAttr = addAttribute(OffsetAttribute.class);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (pos >= tokens.length)
            return false;

        termAttr.setEmpty();
        termAttr.append(tokens[pos]);

        posIncrAttr.setPositionIncrement(positionIncrements[pos]);

        offsetAttr.setOffset(startOffsets[pos], endOffsets[pos]);

        pos++;
        return true;
    }

    @Override
    public void end() throws IOException {
        termAttr.setEmpty();
        posIncrAttr.setPositionIncrement(0);
    }

    @Override
    public void reset() throws IOException {
        pos = 0;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
