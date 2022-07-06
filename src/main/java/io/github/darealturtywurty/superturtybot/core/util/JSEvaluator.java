package io.github.darealturtywurty.superturtybot.core.util;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;

import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;

public final class JSEvaluator {
    private static final Engine ENGINE = Engine.newBuilder("js").option("js.ecmascript-version", "2020")
        .allowExperimentalOptions(true).option("js.console", "true").option("js.nashorn-compat", "true")
        .option("js.experimental-foreign-object-prototype", "true").option("js.disable-eval", "true")
        .option("js.load", "false").option("log.level", "OFF").build();
    
    static {
        ShutdownHooks.register(ENGINE::close);
    }
    
    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder().allowArrayAccess(true).allowListAccess(true)
        .allowMapAccess(true).build();
    
    private JSEvaluator() {
        throw new IllegalAccessError("This is illegal, expect police at your door in 2-5 minutes!");
    }
    
    public static Context getContext() {
        return getContext(Collections.emptyMap());
    }
    
    public static Context getContext(Map<String, Object> additionalBindings) {
        final var ctx = Context.newBuilder("js").engine(ENGINE).allowNativeAccess(false).allowIO(false)
            .allowCreateProcess(false).allowEnvironmentAccess(EnvironmentAccess.NONE).allowHostClassLoading(false)
            .allowValueSharing(true).allowHostAccess(HOST_ACCESS).build();
        
        final var bindings = ctx.getBindings("js");
        bindings.removeMember("load");
        bindings.removeMember("loadWithNewGlobal");
        bindings.removeMember("eval");
        bindings.removeMember("exit");
        bindings.removeMember("quit");
        for (final Entry<String, Object> keyVal : additionalBindings.entrySet()) {
            bindings.putMember(keyVal.getKey(), keyVal.getValue());
        }
        
        return ctx;
    }
}
