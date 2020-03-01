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
    SEND_NEW_REPLICA{
        @Override
        public String hasPayload() {
            return ADDRESS_TRACKER_INDEX;
        }
    },
    REMOVE_OLD_REPLICA{
        @Override
        public String hasPayload() {
            return ADDRESS_TRACKER_INDEX;
        }
    },
    SEND_OTHER_REPLICAS{
        @Override
        public String hasPayload() {
            return ADDRESS_SET_TRACKER_INDEX;
        }
    },
    READ_FROM_CLIENT {
        @Override
        public String hasPayload() {
            return READ;
        }
    },
    WRITE_FROM_CLIENT {
        @Override
        public String hasPayload() {
            return KEY_VALUE;
        }
    },
    READ_ANSWER {
        @Override
        public String hasPayload() {
            return KEY_VALUE;
        }
    },
    GET_STATE{
        @Override
        public String hasPayload() {
            return TRACKER_INDEX;
        }
    },
    SEND_STATE{
        @Override
        public String hasPayload() {
            return STATE;
        }

    },
    NOT_STATE{
        @Override
        public String hasPayload() {
            return null;
        }
    },
    UPDATE_FROM_REPLICA{
        @Override
        public String hasPayload() {
            return UPDATE;
        }
    },
    REPLY_CLIENT{
        @Override
        public String hasPayload() {
            return KEY_VALUE;
        }
    },
    WAIT {
        @Override
        public String hasPayload() {
            return null;
        }
    },
    ACK {
        @Override
        public String hasPayload() {
            return null;
        }
    };

    public static final String ADDRESS_TRACKER_INDEX = "ADDRESS_TRACKER_INDEX";
    public static final String ADDRESS = "ADDRESS";
    public static final String ADDRESS_SET_TRACKER_INDEX = "ADDRESS_SET_TRACKER_INDEX";
    public static final String READ = "READ";
    public static final String KEY_VALUE = "KEY_VALUE";
    public static final String STATE = "STATE";
    public static final String UPDATE = "UPDATE";
    public static final String TRACKER_INDEX = "TRACKER_INDEX";

    public abstract String hasPayload();
}
