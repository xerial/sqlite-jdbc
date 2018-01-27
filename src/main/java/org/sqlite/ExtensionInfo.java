package org.sqlite;

import java.util.Collection;
import java.util.HashSet;
import org.sqlite.util.StringUtils;

public class ExtensionInfo {

    private static final char SERIALIZE_ESCAPE_CHAR = '|';
    private static final char SERIALIZE_EXTENSION_INFO_SEPARATOR = '*';
    private static final char SERIALIZE_FILE_ENTRY_SEPARATOR = '"';

    private final String file;
    private final String entry;

    /*
     * <serialized> ::= ( <file_entry> SERIALIZE_EXTENSION_INFO_SEPARATOR )*
     * <file_entry> ::= <file> [ SERIALIZE_FILE_ENTRY_SEPARATOR <entry> ]
     */
    static String serialize(Collection<ExtensionInfo> extensionInfos) {
        final char[] serializeSpecialChars = new char[] {
                SERIALIZE_EXTENSION_INFO_SEPARATOR,
                SERIALIZE_FILE_ENTRY_SEPARATOR
        };
        StringBuilder builder = new StringBuilder();
        for (ExtensionInfo ei : extensionInfos) {
            builder.append(StringUtils.escape(ei.getFile(), SERIALIZE_ESCAPE_CHAR, serializeSpecialChars));
            if (ei.getEntry() != null) {
                builder.append(SERIALIZE_FILE_ENTRY_SEPARATOR);
                builder.append(StringUtils.escape(ei.getEntry(), SERIALIZE_ESCAPE_CHAR, serializeSpecialChars));
            }
            builder.append(SERIALIZE_EXTENSION_INFO_SEPARATOR);
        }
        return builder.toString();
    }

    static Collection<ExtensionInfo> deserialize(String extensionInfosSerialized) {
        Collection<ExtensionInfo> eis = new HashSet<ExtensionInfo>();
        StringBuilder file = new StringBuilder();
        StringBuilder entry = new StringBuilder();
        boolean parseFile = true;
        boolean esc = false;
        for (int i = 0; i < extensionInfosSerialized.length(); i++) {
            char ch = extensionInfosSerialized.charAt(i);
            if (esc) {
                if (parseFile) {
                    file.append(ch);
                } else {
                    entry.append(ch);
                }
                esc = false;
            } else if (parseFile) {
                assert (entry.length() == 0);
                switch (ch) {
                case SERIALIZE_ESCAPE_CHAR:
                    esc = true;
                    break;
                case SERIALIZE_EXTENSION_INFO_SEPARATOR:
                    eis.add(new ExtensionInfo(file.toString(), null));
                    file.setLength(0);
                    break;
                case SERIALIZE_FILE_ENTRY_SEPARATOR:
                    parseFile = false;
                    break;
                default:
                    file.append(ch);
                    break;
                }
            } else {
                switch (ch) {
                case SERIALIZE_ESCAPE_CHAR:
                    esc = true;
                    break;
                case SERIALIZE_EXTENSION_INFO_SEPARATOR:
                    eis.add(new ExtensionInfo(file.toString(), entry.toString()));
                    file.setLength(0);
                    entry.setLength(0);
                    parseFile = true;
                    break;
                case SERIALIZE_FILE_ENTRY_SEPARATOR:
                    throw new IllegalStateException("Unexpected SERIALIZE_FILE_ENTRY_SEPARATOR: " + extensionInfosSerialized);
                default:
                    entry.append(ch);
                    break;
                }
            }
        }
        // assert last char was unescaped SERIALIZE_EXTENSION_INFO_SEPARATOR
        assert (!esc && parseFile && (file.length() == 0));
        //System.err.println(extensionInfosSerialized);
        //for (ExtensionInfo ei : eis) {
        //    System.err.println("file: " + ei.getFile() + " - entry: " + ei.getEntry());
        //}
        return eis;
    }

    ExtensionInfo(String file, String entry) {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        this.file = file;
        this.entry = entry;
    }

    public String getFile() {
        return file;
    }

    public String getEntry() {
        return entry;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + file.hashCode();
        result = prime * result + ((entry == null) ? 0 : entry.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        ExtensionInfo other = (ExtensionInfo)obj;
        if (!file.equals(other.file)) {
            return false;
        }
        if ((entry == null) != (other.entry == null)) {
            return false;
        }
        if ((entry != null) && !(entry.equals(other.entry))) {
            return false;
        }
        return true;
    }

}
