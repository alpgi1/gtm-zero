package com.gtmzero.config;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Arrays;

/**
 * Hibernate UserType mapping float[] ↔ pgvector's "[x1,x2,...,xN]" wire format.
 *
 * Why Types.OTHER: pgvector registers no implicit text→vector JDBC cast.
 * Binding with Types.OTHER passes the raw string; PostgreSQL's vector_in() function
 * then parses it — the same path pgvector's own JDBC driver examples use.
 * An AttributeConverter<float[],String> would bind as VARCHAR, which pgvector rejects.
 *
 * Uses the Hibernate 7 WrapperOptions overloads (the SharedSessionContractImplementor
 * variants are deprecated in Hibernate 7.x).
 */
public class VectorType implements UserType<float[]> {

    public static final VectorType INSTANCE = new VectorType();

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position,
                               WrapperOptions options) throws SQLException {
        String raw = rs.getString(position);
        if (raw == null) return null;
        // pgvector returns "[x1,x2,...,xN]" — strip brackets and split
        raw = raw.substring(1, raw.length() - 1);
        String[] parts = raw.split(",");
        float[] vec = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Float.parseFloat(parts[i].trim());
        }
        return vec;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index,
                            WrapperOptions options) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < value.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(value[i]);
        }
        sb.append("]");
        // setObject with Types.OTHER lets PostgreSQL route to vector_in()
        st.setObject(index, sb.toString(), Types.OTHER);
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return deepCopy((float[]) cached);
    }
}
