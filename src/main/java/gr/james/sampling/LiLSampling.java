package gr.james.sampling;

import java.util.Random;

/**
 * Implementation of <i>Algorithm L</i> by Li in <b>Reservoir-sampling algorithms of time complexity
 * O(n(1 + log(N/n)))</b>.
 * <p>
 * Unlike {@link WatermanSampling}, the {@link VitterXSampling}, {@link VitterZSampling} and {@code LiLSampling}
 * algorithms decide how many items to skip, rather than deciding whether or not to skip an item each time it is feeded.
 * This property allows these algorithms to perform better by efficiently calculating the number of items that need to
 * be skipped, while making fewer calls to the RNG.
 * <p>
 * This implementation throws {@link StreamOverflowException} if more than {@link Long#MAX_VALUE} items are feeded.
 * <p>
 * The space complexity of this class is {@code O(k)}, where {@code k} is the sample size.
 *
 * @param <T> the item type
 * @author Giorgos Stamatelatos
 * @see <a href="https://doi.org/10.1145/198429.198435">Reservoir-sampling algorithms of time complexity
 * O(n(1 + log(N/n)))</a>
 */
public class LiLSampling<T> extends AbstractRandomSampling<T> {
    private double W;

    /**
     * Construct a new instance of {@link LiLSampling} using the specified sample size and RNG. The implementation
     * assumes that {@code random} conforms to the contract of {@link Random} and will perform no checks to ensure that.
     * If this contract is violated, the behavior is undefined.
     *
     * @param sampleSize the sample size
     * @param random     the RNG to use
     * @throws NullPointerException     if {@code random} is {@code null}
     * @throws IllegalArgumentException if {@code sampleSize} is less than 1
     */
    public LiLSampling(int sampleSize, Random random) {
        super(sampleSize, random);
    }

    /**
     * Construct a new instance of {@link LiLSampling} using the specified sample size and a default source of
     * randomness.
     *
     * @param sampleSize the sample size
     * @throws IllegalArgumentException if {@code sampleSize} is less than 1
     */
    public LiLSampling(int sampleSize) {
        this(sampleSize, new Random());
    }

    /**
     * Get a {@link RandomSamplingCollector} from this class.
     *
     * @param sampleSize the sample size
     * @param random     the RNG to use
     * @param <E>        the type of elements
     * @return a {@link RandomSamplingCollector} from this class
     */
    public static <E> RandomSamplingCollector<E> collector(int sampleSize, Random random) {
        return new RandomSamplingCollector<>(() -> new LiLSampling<>(sampleSize, random));
    }

    @Override
    void init(int sampleSize, Random random) {
        // W = Math.exp(Math.log(RandomSamplingUtils.randomExclusive(random)) / sampleSize);
        W = Math.pow(RandomSamplingUtils.randomExclusive(random), 1.0 / sampleSize);
    }

    @Override
    long skipLength(long streamSize, int sampleSize, Random random) {
        final double random1 = RandomSamplingUtils.randomExclusive(random);
        final double random2 = RandomSamplingUtils.randomExclusive(random);
        long skip = (long) (Math.log(random1) / Math.log(1 - W));
        assert skip >= 0 || skip == Long.MIN_VALUE;
        if (skip == Long.MIN_VALUE) {  // Sometimes when W is very small, 1 - W = 1 and Math.log(1) = +0 instead of -0
            skip = Long.MAX_VALUE;     // This results in negative infinity skip
        }
        // W = W * Math.exp(Math.log(random2) / sampleSize);
        W = W * Math.pow(random2, 1.0 / sampleSize);
        return skip;
    }

    @Deprecated
    private static class LiLSkipFunction implements SkipFunction {
        private final int sampleSize;
        private final Random random;
        private double W;

        public LiLSkipFunction(int sampleSize, Random random) {
            this.sampleSize = sampleSize;
            this.random = random;
            this.W = Math.pow(RandomSamplingUtils.randomExclusive(random), 1.0 / sampleSize);
        }

        @Override
        public long skip() throws StreamOverflowException {
            final double random1 = RandomSamplingUtils.randomExclusive(random);
            final double random2 = RandomSamplingUtils.randomExclusive(random);
            long skip = (long) (Math.log(random1) / Math.log(1 - W));
            assert skip >= 0 || skip == Long.MIN_VALUE;
            if (skip == Long.MIN_VALUE) {  // Sometimes when W is very small, 1 - W = 1 and Math.log(1) = +0 instead of -0
                skip = Long.MAX_VALUE;     // This results in negative infinity skip
            }
            // W = W * Math.exp(Math.log(random2) / sampleSize);
            W = W * Math.pow(random2, 1.0 / sampleSize);
            return skip;
        }
    }
}
