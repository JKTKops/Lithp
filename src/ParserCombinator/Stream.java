package ParserCombinator;

class Stream {
    String string;
    int cursor = 0;
    int length;

    Stream(String s, int c, int l) {
        string = s;
        cursor = c;
        length = l;
    }

    String head() {
        if (length <= 0) { throw new IllegalStateException("Stream is empty."); }
        return ((Character) string.charAt(cursor)).toString();
    }

    Stream move(int distance) {
        return new Stream(string, cursor + distance, length - distance);
    }

    Stream slice(int start, int stop) {
        if (stop < start) { throw new IllegalArgumentException("stop < start"); }
        if (start < 0 || stop > length) { throw new IllegalArgumentException("Index out of range"); }
        return new Stream(string, cursor + start, stop - start);
    }
    Stream slice(int start) {
        if (start < 0) {throw new IllegalArgumentException("Index out of range"); }
        return new Stream(string, cursor + start, length - start);
    }
}
