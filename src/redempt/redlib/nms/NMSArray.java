package redempt.redlib.nms;

import java.lang.reflect.Array;

/**
 * Wraps any type of Array and provides easy reflection access
 *
 * @author Redempt
 */
public class NMSArray {

    private Object array;

    public NMSArray(Object array) {
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Object passed is not an array!");
        }
        this.array = array;
    }

    /**
     * Gets a wrapped NMSObject with the value at a certain index in the array
     *
     * @param index The index to get
     * @return An NMSObject wrapping the object at the index
     */
    public NMSObject get(int index) {
        return new NMSObject(Array.get(array, index));
    }

    /**
     * Gets the object at the given index in the wrapped array
     *
     * @param index The index to get
     * @return The object at the index
     */
    public Object getDirect(int index) {
        return Array.get(array, index);
    }

    /**
     * Sets the object at the index of the wrapped array
     *
     * @param index The index to set
     * @param obj   The object to set. If it is an {@link NMSObject}, it will be unwrapped automatically.
     */
    public void set(int index, Object obj) {
        if (obj instanceof NMSObject) {
            obj = ((NMSObject) obj).getObject();
        }
        Array.set(array, index, obj);
    }

    public int length() {
        return Array.getLength(array);
    }

}
