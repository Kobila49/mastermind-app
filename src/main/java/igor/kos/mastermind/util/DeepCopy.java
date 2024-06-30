package igor.kos.mastermind.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DeepCopy {

    private DeepCopy() {
    }

    public static Map<Integer, Integer[]> deepCopyMap(Map<Integer, Integer[]> originalMap) {
        Map<Integer, Integer[]> copiedMap = new HashMap<>();
        for (Map.Entry<Integer, Integer[]> entry : originalMap.entrySet()) {
            Integer[] originalArray = entry.getValue();
            Integer[] copiedArray = Arrays.copyOf(originalArray, originalArray.length);
            copiedMap.put(entry.getKey(), copiedArray);
        }
        return copiedMap;
    }

    public static Integer[] deepCopyArray(Integer[] originalArray) {
        return Arrays.copyOf(originalArray, originalArray.length);
    }
}
