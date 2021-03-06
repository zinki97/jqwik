package net.jqwik.engine.properties.arbitraries.randomized;

import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.engine.properties.*;
import net.jqwik.engine.properties.shrinking.*;

public class RandomGenerators {

	public static final int DEFAULT_COLLECTION_SIZE = 255;

	public static <U> RandomGenerator<U> choose(List<U> values) {
		if (values.size() == 0) {
			return fail("empty set of values");
		}
		return random -> {
			U value = chooseValue(values, random);
			return new ChooseValueShrinkable<>(value, values);
		};
	}

	public static <U> U chooseValue(List<U> values, Random random) {
		int index = random.nextInt(values.size());
		return values.get(index);
	}

	public static <U> RandomGenerator<U> choose(U[] values) {
		return choose(Arrays.asList(values));
	}

	public static RandomGenerator<Character> choose(char[] characters) {
		List<Character> validCharacters = new ArrayList<>(characters.length);
		for (char character : characters) {
			validCharacters.add(character);
		}
		return choose(validCharacters);
	}

	public static RandomGenerator<Character> chars(char min, char max) {
		return integers(min, max).map(anInt -> ((char) (int) anInt));
	}

	public static RandomGenerator<Integer> integers(int min, int max) {
		BigInteger minBig = BigInteger.valueOf(min);
		BigInteger maxBig = BigInteger.valueOf(max);
		return bigIntegers(
				minBig,
				maxBig,
				RandomIntegralGenerators.defaultShrinkingTarget(Range.of(minBig, maxBig)),
				RandomDistribution.uniform()
		).map(BigInteger::intValueExact);
	}

	public static RandomGenerator<BigInteger> bigIntegers(
			BigInteger min,
			BigInteger max,
			BigInteger shrinkingTarget,
			RandomDistribution distribution
	) {
		return RandomIntegralGenerators.bigIntegers(1000, min, max, shrinkingTarget, distribution);
	}

	public static RandomGenerator<BigDecimal> bigDecimals(
			Range<BigDecimal> range,
			int scale,
			BigDecimal shrinkingTarget,
			RandomDistribution distribution
	) {
		return RandomDecimalGenerators.bigDecimals(1000, range, scale, distribution, shrinkingTarget);
	}

	public static <T> RandomGenerator<List<T>> list(
			RandomGenerator<T> elementGenerator, int minSize, int maxSize, Set<FeatureExtractor<T>> uniquenessExtractors, int cutoffSize
	) {
		Function<List<Shrinkable<T>>, Shrinkable<List<T>>> createShrinkable =
				elements -> new ShrinkableList<>(elements, minSize, maxSize, uniquenessExtractors);
		return container(elementGenerator, createShrinkable, minSize, maxSize, cutoffSize, uniquenessExtractors);
	}

	public static <T> RandomGenerator<T> oneOf(List<RandomGenerator<T>> all) {
		return choose(all).flatMap(Function.identity());
	}

	public static <T> RandomGenerator<List<T>> shuffle(List<T> values) {
		return random -> {
			List<T> clone = new ArrayList<>(values);
			Collections.shuffle(clone, random);
			return Shrinkable.unshrinkable(clone);
		};
	}

	public static RandomGenerator<String> strings(
			RandomGenerator<Character> elementGenerator, int minLength, int maxLength, int cutoffLength
	) {
		Function<List<Shrinkable<Character>>, Shrinkable<String>> createShrinkable = elements -> new ShrinkableString(elements, minLength, maxLength);
		return container(elementGenerator, createShrinkable, minLength, maxLength, cutoffLength, Collections.emptySet());
	}

	public static RandomGenerator<String> strings(
			RandomGenerator<Character> elementGenerator, int minLength, int maxLength
	) {
		int defaultCutoff = defaultCutoffSize(minLength, maxLength);
		return strings(elementGenerator, minLength, maxLength, defaultCutoff);
	}

	private static int defaultCutoffSize(int minSize, int maxSize) {
		int range = maxSize - minSize;
		int offset = (int) Math.max(Math.round(Math.sqrt(100)), 10);
		if (range <= offset)
			return maxSize;
		return Math.min(offset + minSize, maxSize);
	}

	private static <T, C> RandomGenerator<C> container(
			RandomGenerator<T> elementGenerator,
			Function<List<Shrinkable<T>>, Shrinkable<C>> createShrinkable,
			int minSize, int maxSize, int cutoffSize,
			Set<FeatureExtractor<T>> uniquenessExtractors
	) {
		return new ContainerGenerator<>(elementGenerator, createShrinkable, minSize, maxSize, cutoffSize, uniquenessExtractors);
	}

	public static <T> RandomGenerator<Set<T>> set(RandomGenerator<T> elementGenerator, int minSize, int maxSize) {
		int defaultCutoffSize = defaultCutoffSize(minSize, maxSize);
		return set(elementGenerator, minSize, maxSize, defaultCutoffSize, Collections.emptySet());
	}

	public static <T> RandomGenerator<Set<T>> set(
			RandomGenerator<T> elementGenerator,
			int minSize, int maxSize, int cutoffSize,
			Set<FeatureExtractor<T>> uniquenessExtractors
	) {
		Set<FeatureExtractor<T>> extractors = new HashSet<>(uniquenessExtractors);
		extractors.add(FeatureExtractor.identity());
		Function<List<Shrinkable<T>>, Shrinkable<Set<T>>> createShrinkable =
				elements -> new ShrinkableSet<T>(elements, minSize, maxSize, uniquenessExtractors);
		return container(elementGenerator, createShrinkable, minSize, maxSize, cutoffSize, extractors);
	}

	public static <T> RandomGenerator<T> samplesFromShrinkables(List<Shrinkable<T>> samples) {
		AtomicInteger tryCount = new AtomicInteger(0);
		return ignored -> {
			if (tryCount.get() >= samples.size())
				tryCount.set(0);
			return samples.get(tryCount.getAndIncrement());
		};
	}

	public static <T> RandomGenerator<T> samples(T[] samples) {
		List<Shrinkable<T>> shrinkables = SampleShrinkable.listOf(samples);
		return samplesFromShrinkables(shrinkables);
	}

	public static <T> RandomGenerator<T> frequency(List<Tuple2<Integer, T>> frequencies) {
		return new FrequencyGenerator<>(frequencies);
	}

	public static <T> RandomGenerator<T> frequencyOf(
			List<Tuple2<Integer, Arbitrary<T>>> frequencies,
			int genSize,
			boolean withEmbeddedEdgeCases
	) {
		return frequency(frequencies).flatMap(Function.identity(), genSize, withEmbeddedEdgeCases);
	}

	public static <T> RandomGenerator<T> withEdgeCases(RandomGenerator<T> self, int genSize, EdgeCases<T> edgeCases) {
		if (edgeCases.isEmpty()) {
			return self;
		}
		return new WithEdgeCasesGenerator<>(self, edgeCases, genSize);
	}

	public static <T> RandomGenerator<T> fail(String message) {
		return ignored -> {
			throw new JqwikException(message);
		};
	}

	public static int defaultCutoffSize(int minSize, int maxSize, int genSize) {
		int range = maxSize - minSize;
		int offset = (int) Math.max(Math.round(Math.sqrt(genSize)), 10);
		if (range <= offset)
			return maxSize;
		return Math.min(offset + minSize, maxSize);
	}
}
