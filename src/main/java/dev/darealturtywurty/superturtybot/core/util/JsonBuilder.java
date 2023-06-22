package dev.darealturtywurty.superturtybot.core.util;

public abstract sealed class JsonBuilder permits JsonBuilder.ArrayBuilder, JsonBuilder.ObjectBuilder {
    public static final class ArrayBuilder extends JsonBuilder {
        public ArrayBuilder() {
            this.builder.append("[");
        }

        @Override
        void finish() {
            this.builder.append("]");
            this.builder.deleteCharAt(this.builder.lastIndexOf(","));
        }

        public ArrayBuilder add(String value) {
            this.builder.append("\"").append(value).append("\",");
            return this;
        }

        public ArrayBuilder add(int value) {
            this.builder.append(value).append(",");
            return this;
        }

        public ArrayBuilder add(double value) {
            this.builder.append(value).append(",");
            return this;
        }

        public ArrayBuilder add(boolean value) {
            this.builder.append(value).append(",");
            return this;
        }

        public ArrayBuilder add(float value) {
            this.builder.append(value).append(",");
            return this;
        }

        public ArrayBuilder add(char value) {
            this.builder.append(value).append(",");
            return this;
        }

        public ArrayBuilder add(long value) {
            this.builder.append(value).append(",");
            return this;
        }

        public ArrayBuilder add(JsonBuilder value) {
            this.builder.append(value.toJson()).append(",");
            return this;
        }

        public ArrayBuilder add(Object value) {
            this.builder.append(Constants.GSON.toJson(value)).append(",");
            return this;
        }
    }

    public static final class ObjectBuilder extends JsonBuilder {
        public ObjectBuilder() {
            this.builder.append("{");
        }

        @Override
        void finish() {
            this.builder.append("}");
            this.builder.deleteCharAt(this.builder.lastIndexOf(","));
        }

        public ObjectBuilder add(String key, CharSequence value) {
            this.builder.append("\"").append(key).append("\":\"").append(value).append("\",");
            return this;
        }

        public ObjectBuilder add(String key, int value) {
            this.builder.append("\"").append(key).append("\":").append(value).append(",");
            return this;
        }

        public ObjectBuilder add(String key, double value) {
            this.builder.append("\"").append(key).append("\":").append(value).append(",");
            return this;
        }

        public ObjectBuilder add(String key, boolean value) {
            this.builder.append("\"").append(key).append("\":").append(value).append(",");
            return this;
        }

        public ObjectBuilder add(String key, float value) {
            this.builder.append("\"").append(key).append("\":").append(value).append(",");
            return this;
        }

        public ObjectBuilder add(String key, char value) {
            this.builder.append("\"").append(key).append("\":").append(value).append(",");
            return this;
        }

        public ObjectBuilder add(String key, long value) {
            this.builder.append("\"").append(key).append("\":").append(value).append(",");
            return this;
        }

        public ObjectBuilder add(String key, JsonBuilder value) {
            this.builder.append("\"").append(key).append("\":").append(value.toJson()).append(",");
            return this;
        }

        public ObjectBuilder add(String key, Object value) {
            this.builder.append("\"").append(key).append("\":").append(Constants.GSON.toJson(value)).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, String... values) {
            var arrayBuilder = new ArrayBuilder();
            for (String value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, int... values) {
            var arrayBuilder = new ArrayBuilder();
            for (int value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, double... values) {
            var arrayBuilder = new ArrayBuilder();
            for (double value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, boolean... values) {
            var arrayBuilder = new ArrayBuilder();
            for (boolean value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, float... values) {
            var arrayBuilder = new ArrayBuilder();
            for (float value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, char... values) {
            var arrayBuilder = new ArrayBuilder();
            for (char value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, long... values) {
            var arrayBuilder = new ArrayBuilder();
            for (long value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, JsonBuilder... values) {
            var arrayBuilder = new ArrayBuilder();
            for (JsonBuilder value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }

        public ObjectBuilder addArray(String key, Object... values) {
            var arrayBuilder = new ArrayBuilder();
            for (Object value : values) {
                arrayBuilder.add(value);
            }

            this.builder.append("\"").append(key).append("\":").append(arrayBuilder.toJson()).append(",");
            return this;
        }
    }

    public static ArrayBuilder array() {
        return new ArrayBuilder();
    }

    public static ObjectBuilder object() {
        return new ObjectBuilder();
    }

    final StringBuilder builder = new StringBuilder();

    private JsonBuilder() {}

    public final String toJson() {
        finish();
        return this.builder.toString();
    }

    abstract void finish();
}