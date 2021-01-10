package org.infinity.rpc.core.exchange.serialization.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.infinity.rpc.core.exchange.serialization.Serializer;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hession2序列化，要求序列化的对象实现 java.io.Serializable 接口
 */
@ServiceName("hessian2")
public class Hessian2Serializer implements Serializer {
    @Override
    public byte[] serialize(Object data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        out.writeObject(data);
        out.flush();
        return bos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(data));
        return (T) input.readObject(clz);
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        for (Object obj : data) {
            out.writeObject(obj);
        }
        out.flush();
        return bos.toByteArray();
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(data));
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            objects[i] = input.readObject(classes[i]);
        }
        return objects;
    }

    @Override
    public int getSerializationTypeNum() {
        return 0;
    }
}
