package uk.co.xfour.kimjongun3;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Location;

/**
 * Provides helpers for interacting with Kim Jong Un 3 Nova blocks.
 */
public class KimJongUnBlocks {
    private static final String LAUNCHPAD_ID = "kimjongun3:launchpad";
    private static final String MISSILE_ID = "kimjongun3:missile";
    private static final String ICBM_ID = "kimjongun3:icbm";
    private static final String BLOCK_MANAGER_CLASS = "xyz.xenondevs.nova.api.ApiBlockManager";

    private final Object blockManager;
    private final Method hasBlockMethod;
    private final Method getBlockMethod;

    public KimJongUnBlocks() {
        try {
            Class<?> managerClass = Class.forName(BLOCK_MANAGER_CLASS);
            Field instanceField = managerClass.getField("INSTANCE");
            this.blockManager = instanceField.get(null);
            this.hasBlockMethod = managerClass.getMethod("hasBlock", Location.class);
            this.getBlockMethod = managerClass.getMethod("getBlock", Location.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nova block manager is unavailable.", e);
        }
    }

    public boolean isLaunchpad(Location location) {
        return hasBlockId(location, LAUNCHPAD_ID);
    }

    public boolean isMissile(Location location) {
        return hasBlockId(location, MISSILE_ID);
    }

    public boolean isIcbm(Location location) {
        return hasBlockId(location, ICBM_ID);
    }

    private boolean hasBlockId(Location location, String id) {
        try {
            boolean hasBlock = (boolean) hasBlockMethod.invoke(blockManager, location);
            if (!hasBlock) {
                return false;
            }
            Object state = getBlockMethod.invoke(blockManager, location);
            if (state == null) {
                return false;
            }
            Object block = invokeNoArg(state, "getBlock");
            if (block == null) {
                return false;
            }
            Object blockId = invokeNoArg(block, "getId");
            return blockId != null && blockId.toString().equals(id);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    private Object invokeNoArg(Object target, String methodName) throws IllegalAccessException,
        InvocationTargetException {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
