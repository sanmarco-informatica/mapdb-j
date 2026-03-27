package org.mapdb.serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.elsa.ElsaSerializerPojo;

public final class SerializerElsa extends GroupSerializerObjectArray<Object> {
    @NotNull
    private final ElsaSerializerPojo ser = new ElsaSerializerPojo();

    @Nullable
    public Object deserialize(@NotNull DataInput2 input, int available) throws IOException {
        return this.ser.deserialize((DataInput)input);
    }

    public void serialize(@NotNull DataOutput2 out, @NotNull Object value) throws IOException {
        this.ser.serialize((DataOutput)out, value);
    }
}
