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
    private static final String BLOCK_REGISTRY_CLASS = "xyz.xenondevs.nova.api.ApiBlockRegistry";

    private final Object blockManager;
    private final Method hasBlockMethod;
    private final Method getBlockMethod;
    private final Method placeBlockMethod;
    private final Object blockRegistry;
    private final Method getBlockByIdMethod;
    private final boolean available;

    public KimJongUnBlocks() {
        Object manager = null;
        Method hasBlock = null;
        Method getBlock = null;
        Method placeBlock = null;
        Object registry = null;
        Method getBlockById = null;
        boolean isAvailable = false;
        try {
            Class<?> managerClass = Class.forName(BLOCK_MANAGER_CLASS);
            Field instanceField = managerClass.getField("INSTANCE");
            manager = instanceField.get(null);
            hasBlock = managerClass.getMethod("hasBlock", Location.class);
            getBlock = managerClass.getMethod("getBlock", Location.class);
            placeBlock = resolvePlaceBlockMethod(managerClass);
            Class<?> registryClass = Class.forName(BLOCK_REGISTRY_CLASS);
            Field registryInstanceField = registryClass.getField("INSTANCE");
            registry = registryInstanceField.get(null);
            getBlockById = registryClass.getMethod("get", String.class);
            isAvailable = manager != null && hasBlock != null && getBlock != null && placeBlock != null
                && registry != null && getBlockById != null;
        } catch (ReflectiveOperationException e) {
            isAvailable = false;
        }
        this.blockManager = manager;
        this.hasBlockMethod = hasBlock;
        this.getBlockMethod = getBlock;
        this.placeBlockMethod = placeBlock;
        this.blockRegistry = registry;
        this.getBlockByIdMethod = getBlockById;
        this.available = isAvailable;
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

    public boolean placeLaunchpad(Location location, Object source) {
        return placeBlockById(location, LAUNCHPAD_ID, source);
    }

    private boolean hasBlockId(Location location, String id) {
        if (!available) {
            return false;
        }
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

    private boolean placeBlockById(Location location, String id, Object source) {
        if (!available) {
            return false;
        }
        try {
            Object block = getBlockByIdMethod.invoke(blockRegistry, id);
            if (block == null) {
                return false;
            }
            Method method = resolvePlaceBlockMethod(placeBlockMethod, block.getClass());
            if (method == null) {
                return false;
            }
            method.invoke(blockManager, location, block, source, true);
            return true;
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

    private Method resolvePlaceBlockMethod(Class<?> managerClass) {
        for (Method method : managerClass.getMethods()) {
            if (!method.getName().equals("placeBlock")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 4 && params[0] == Location.class && params[3] == boolean.class) {
                return method;
            }
        }
        return null;
    }

    private Method resolvePlaceBlockMethod(Method current, Class<?> blockClass) {
        if (current != null) {
            Class<?>[] params = current.getParameterTypes();
            if (params.length == 4 && params[1].isAssignableFrom(blockClass)) {
                return current;
            }
        }
        if (blockManager == null) {
            return null;
        }
        for (Method method : blockManager.getClass().getMethods()) {
            if (!method.getName().equals("placeBlock")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 4 && params[0] == Location.class && params[3] == boolean.class
                && params[1].isAssignableFrom(blockClass)) {
                return method;
            }
        }
        return null;
    }
}
