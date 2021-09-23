package github.tornaco.thanox.android.server.patch.framework.hooks.am;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;

import com.android.dx.stock.BaseProxyFactory;
import com.android.dx.stock.ProxyBuilder;
import com.android.server.firewall.IntentFirewall;
import com.elvishew.xlog.XLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import github.tornaco.android.thanos.core.util.AbstractSafeR;
import github.tornaco.android.thanos.services.BootStrap;
import util.XposedHelpers;

class IFWHooks {

    static void installIFW(Object ams) {
        // public final IntentFirewall mIntentFirewall;
        // mIntentFirewall = new IntentFirewall(new IntentFirewallInterface(), mHandler);
        // this.mAms = ams;
        new AbstractSafeR() {
            @Override
            public void runSafety() {
                XposedHelpers.setObjectField(
                        ams, "mIntentFirewall", IFWProxyBuilder.proxy(ams));
                XLog.w("IFWHooks installIFW done");
            }
        }.setName("IFWHooks installIFW").run();
    }


    private static class IFWProxyBuilder {
        @SuppressWarnings("unchecked")
        @Nullable
        public static Object proxy(Object ams) {
            // IFW.
            // public /*final*/ IntentFirewall mIntentFirewall;
            IntentFirewall ifw = (IntentFirewall) XposedHelpers.getObjectField(ams, "mIntentFirewall");
            XLog.i("IFWHooks IFWProxyBuilder installHooksForAMS, ifw: %s", ifw);
            IntentFirewall.AMSInterface amsInterface =
                    (IntentFirewall.AMSInterface) XposedHelpers.getObjectField(ifw, "mAms");
            Handler handler = (Handler) XposedHelpers.getObjectField(ifw, "mHandler");
            return new IWFProxyFactory(amsInterface, handler).newProxy(ifw);
        }

        @SuppressWarnings("rawtypes")
        private static class IWFProxyFactory extends BaseProxyFactory {
            private final IntentFirewall.AMSInterface amsInterface;
            private final Handler handler;

            private IWFProxyFactory(IntentFirewall.AMSInterface amsInterface,
                                    Handler handler) {
                this.amsInterface = amsInterface;
                this.handler = handler;
            }

            @Override
            protected Object onCreateProxy(Object local, File dexCacheDir) throws Exception {
                return newProxy0(local, dexCacheDir);
            }

            private Object newProxy0(final Object local, File dexCacheDir) throws IOException {
                if (local == null) return null;
                XLog.i("IFWHooks IWFProxyFactory#newProxy0, local: %s", local);

                return ProxyBuilder.forClass(IntentFirewall.class)
                        .dexCache(dexCacheDir)
                        // public IntentFirewall(AMSInterface ams, Handler handler)
                        .constructorArgTypes(IntentFirewall.AMSInterface.class, Handler.class)
                        .constructorArgValues(amsInterface, handler)
                        .withSharedClassLoader()
                        .markTrusted()
                        .handler(new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                method.setAccessible(true);

                                XLog.v("IFWHooks: %s %s", method.getName(), Arrays.toString(args));

                                try {
                                    if ("checkService".equals(method.getName())) {
                                        Boolean hookRes = handleCheckService(args);
                                        if (hookRes != null && !hookRes) {
                                            return false;
                                        }
                                    }

                                    if ("checkBroadcast".equals(method.getName())) {
                                        Boolean hookRes = handleCheckBroadcast(args);
                                        if (hookRes != null && !hookRes) {
                                            return false;
                                        }
                                    }
                                } catch (Throwable e) {
                                    XLog.e("IFWHooks IWFProxy error", e);
                                }

                                return method.invoke(local, args);
                            }
                        })
                        .build();
            }

            private Boolean handleCheckBroadcast(Object[] args) {
                int callerUid = (int) args[1];
                int recUid = (int) args[4];
                Intent intent = (Intent) args[0];
                if (intent == null) return null;
                boolean res =
                        BootStrap.THANOS_X
                                .getActivityManagerService()
                                .checkBroadcast(intent, recUid, callerUid);
                if (!res) {
                    return false;
                }
                return null;
            }

            private Boolean handleCheckService(Object[] args) {
                Intent intent = (Intent) args[1];
                ComponentName componentName = (ComponentName) args[0];
                if (componentName == null) {
                    return null;
                }
                int callerID = (int) args[2];
                boolean res =
                        BootStrap.THANOS_X
                                .getActivityManagerService()
                                .checkService(intent, componentName, callerID);
                if (!res) {
                    return false;
                }
                return null;
            }
        }
    }
}
