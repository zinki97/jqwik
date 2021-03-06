package net.jqwik.api.arbitraries;

import java.util.function.*;
import java.util.stream.*;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

/**
 * Fluent interface to add functionality to arbitraries that generate instances
 * of type {@linkplain Stream}
 */
@API(status = MAINTAINED, since = "1.3.2")
public interface StreamArbitrary<T> extends StreamableArbitrary<T, Stream<T>> {

	/**
	 * Fix the size to {@code size}.
	 *
	 * @param size The size of the generated stream
	 * @return new arbitrary instance
	 */
	@Override
	default StreamArbitrary<T> ofSize(int size) {
		return ofMinSize(size).ofMaxSize(size);
	}

	/**
	 * Set lower size boundary {@code minSize} (included).
	 *
	 * @param minSize The minimum size of the generated stream
	 * @return new arbitrary instance
	 */
	StreamArbitrary<T> ofMinSize(int minSize);

	/**
	 * Set upper size boundary {@code maxSize} (included).
	 *
	 * @param maxSize The maximum size of the generated stream
	 * @return new arbitrary instance
	 */
	StreamArbitrary<T> ofMaxSize(int maxSize);

	/**
	 * Add the constraint that elements of the generated stream must be unique,
	 * i.e. no two elements must return true when being compared using {@linkplain Object#equals(Object)}.
	 *
	 * <p>
	 *     The constraint can be combined with other {@linkplain #uniqueElements(Function)} constraints.
	 * </p>
	 *
	 * @return new arbitrary instance
	 */
	@API(status = MAINTAINED, since = "1.4.0")
	StreamArbitrary<T> uniqueElements();

	/**
	 * Add the constraint that elements of the generated stream must be unique
	 * relating to an element's "feature" being extracted using the
	 * {@code by} function.
	 * The extracted features are being compared using {@linkplain Object#equals(Object)}.
	 *
	 * <p>
	 *     The constraint can be combined with other {@linkplain #uniqueElements(Function)} constraints.
	 * </p>
	 *
	 * @return new arbitrary instance
	 */
	@API(status = MAINTAINED, since = "1.4.0")
	StreamArbitrary<T> uniqueElements(Function<T, Object> by);

}
