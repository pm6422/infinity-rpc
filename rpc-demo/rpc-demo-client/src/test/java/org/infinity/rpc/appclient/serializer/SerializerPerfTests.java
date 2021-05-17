package org.infinity.rpc.appclient.serializer;

import lombok.extern.slf4j.Slf4j;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.infinity.rpc.appclient.serializer.service.AppServiceImpl;
import org.infinity.rpc.democommon.domain.App;
import org.infinity.rpc.spring.enhancement.kryo.serializer.*;
import org.infinity.rpc.utilities.serializer.Serializer;
import org.infinity.rpc.utilities.serializer.kryo.KryoUtils;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.domain.*;

import java.io.IOException;

import static org.infinity.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;
import static org.infinity.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_KRYO;

@Slf4j
public class SerializerPerfTests {

    @Rule
    public  ContiPerfRule i                  = new ContiPerfRule();
    private Serializer    kryoSerializer     = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_KRYO);
    private Serializer    hessian2Serializer = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_HESSIAN2);

    static {
        KryoUtils.registerClass(Sort.class, new SortSerializer());
        KryoUtils.registerClass(PageRequest.class, new PageRequestSerializer());
        KryoUtils.registerClass(Pageable.class, new PageableSerializer());
        KryoUtils.registerClass(PageImpl.class, new PageImplSerializer());
        KryoUtils.registerClass(Page.class, new PageSerializer());
    }

    @Test
    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
    public void kryoPerf() throws IOException {
        AppServiceImpl appServiceImpl = new AppServiceImpl();
        Pageable pageable = PageRequest.of(0, 100, Sort.by("name").descending());
        Page<App> all = appServiceImpl.findAll(pageable);
        byte[] serialized = kryoSerializer.serialize(all);
        kryoSerializer.deserialize(serialized, Page.class);
    }

    @Test
    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
    public void hessian2Perf() throws IOException {
        AppServiceImpl appServiceImpl = new AppServiceImpl();
        Pageable pageable = PageRequest.of(0, 100, Sort.by("name").descending());
        Page<App> all = appServiceImpl.findAll(pageable);
        byte[] serialized = hessian2Serializer.serialize(all);
        hessian2Serializer.deserialize(serialized, Page.class);
    }
}
