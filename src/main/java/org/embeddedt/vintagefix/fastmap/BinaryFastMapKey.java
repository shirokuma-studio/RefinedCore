package org.embeddedt.vintagefix.fastmap;

import com.google.common.base.Preconditions;
import net.minecraft.block.properties.IProperty;
import net.minecraft.util.math.MathHelper;

/**
 * A bitmask-based implementation of a FastMapKey. This reduces the density of data in the value matrix, but allows
 * accessing values with only some bitwise operations, which are much faster than integer division
 */
public class BinaryFastMapKey<T extends Comparable<T>> extends FastMapKey<T> {
    private final byte firstBitInValue;
    private final byte firstBitAfterValue;

    private static boolean isPowerOfTwo(int value)
    {
        return value != 0 && (value & value - 1) == 0;
    }

    public BinaryFastMapKey(IProperty<T> property, int mapFactor) {
        super(property);
        Preconditions.checkArgument(isPowerOfTwo(mapFactor));
        final int addedFactor = MathHelper.smallestEncompassingPowerOfTwo(numValues());
        Preconditions.checkState(numValues() <= addedFactor);
        Preconditions.checkState(addedFactor < 2 * numValues());
        final int setBitInBaseFactor = MathHelper.log2(mapFactor);
        final int setBitInAddedFactor = MathHelper.log2(addedFactor);
        Preconditions.checkState(setBitInBaseFactor + setBitInAddedFactor <= 31);
        firstBitInValue = (byte) setBitInBaseFactor;
        firstBitAfterValue = (byte) (setBitInBaseFactor + setBitInAddedFactor);
    }

    @Override
    public T getValue(int mapIndex) {
        final int clearAbove = mapIndex & lowestNBits(firstBitAfterValue);
        return byInternalIndex(clearAbove >>> firstBitInValue);
    }

    @Override
    public int replaceIn(int mapIndex, T newValue) {
        final int newPartialIndex = toPartialMapIndex(newValue);
        if (newPartialIndex < 0) {
            return -1;
        }
        final int keepMask = ~lowestNBits(firstBitAfterValue) | lowestNBits(firstBitInValue);
        return (keepMask & mapIndex) | newPartialIndex;
    }

    @Override
    public int toPartialMapIndex(Comparable<?> value) {
        final int internalIndex = getInternalIndex(value);
        if (internalIndex < 0 || internalIndex >= numValues()) {
            return -1;
        } else {
            return internalIndex << firstBitInValue;
        }
    }

    @Override
    public int getFactorToNext() {
        return 1 << (firstBitAfterValue - firstBitInValue);
    }

    private int lowestNBits(byte n) {
        if (n >= Integer.SIZE) {
            return -1;
        } else {
            return (1 << n) - 1;
        }
    }
}
