package it.polimi.ds.network;

public enum MessageType {
    ADD_REPLICA{
        @Override
        public String hasPayload() {
            return ADDRESS;
        }
    },
    ADD_CLIENT{
        @Override
        public String hasPayload() {
            return null;
        }
    },
    REMOVE_REPLICA{
        @Override
        public String hasPayload() {
            return ADDRESS;
        }
    },
    REMOVE_CLIENT{
        @Override
        public String hasPayload() {
            return ADDRESS;
        }
    },
    SEND_REPLICA{
        @Override
        public String hasPayload() {
            return ADDRESS;
        }
    },
    SEND_OTHER_REPLICAS{
        @Override
        public String hasPayload() {
            return ADDRESS_SET;
        }
    },
    READ_REPLICA{
        @Override
        public String hasPayload() {
            return READ;
        }
    },
    WRITE_REPLICA{
        @Override
        public String hasPayload() {
            return WRITE;
        }
    };

    public static final String ADDRESS = "ADDRESS";
    public static final String ADDRESS_SET = "ADDRESS_SET";
    public static final String READ = "READ";
    public static final String WRITE = "WRITE";

    public abstract String hasPayload();
}
