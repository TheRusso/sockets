package org.example.common;

import java.util.Iterator;

public class SegmentedBuffer implements Iterator<String> {
    private final String terminator;
    private String buffer = "";
    private boolean isFlushing = false;

    public SegmentedBuffer() {
        this("\n");
    }

    public SegmentedBuffer(String terminator) {
        this.terminator = terminator;
    }

    public void put(CharSequence charSequence) {
        buffer += charSequence;
    }

    public void flush() {
        isFlushing = !buffer.isEmpty();
    }

    @Override
    public boolean hasNext() {
        return isFlushing || buffer.contains(terminator);
    }

    @Override
    public String next() {
        if (isFlushing) {
            isFlushing = false;
            String line = buffer;
            buffer = "";
            return line;
        }

        int index = buffer.indexOf(terminator);
        String line = buffer.substring(0, index);
        buffer = buffer.substring(index + 1);
        return line;
    }

}
