package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.engine.support.*;

public class CollectShrinkable<T> implements Shrinkable<List<T>> {
	private final List<Shrinkable<T>> elements;
	private final Predicate<List<T>> until;

	public CollectShrinkable(List<Shrinkable<T>> elements, Predicate<List<T>> until) {
		this.elements = elements;
		this.until = until;
	}

	@Override
	public List<T> value() {
		return createValue(elements);
	}

	private List<T> createValue(List<Shrinkable<T>> elements) {
		return elements
				   .stream()
				   .map(Shrinkable::value)
				   .collect(Collectors.toList());
	}

	@Override
	public Stream<Shrinkable<List<T>>> shrink() {
		return JqwikStreamSupport.concat(
			shrinkElementsOneAfterTheOther(),
			sortElements()
		).filter(s -> until.test(s.value()));
	}

	private Stream<Shrinkable<List<T>>> shrinkElementsOneAfterTheOther() {
		List<Stream<Shrinkable<List<T>>>> shrinkPerPartStreams = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++) {
			int index = i;
			Shrinkable<T> part = elements.get(i);
			Stream<Shrinkable<List<T>>> shrinkElement = part.shrink().flatMap(shrunkElement -> {
				Optional<List<Shrinkable<T>>> shrunkCollection = collectElements(index, shrunkElement);
				return shrunkCollection
						   .map(shrunkElements -> Stream.of(createShrinkable(shrunkElements)))
						   .orElse(Stream.empty());
			});
			shrinkPerPartStreams.add(shrinkElement);
		}
		return JqwikStreamSupport.concat(shrinkPerPartStreams);
	}

	// TODO: Remove duplication with ShrinkableContainer.sortElements() and
	protected Stream<Shrinkable<List<T>>> sortElements() {
		List<Shrinkable<T>> sortedElements = new ArrayList<>(elements);
		sortedElements.sort(Comparator.comparing(Shrinkable::distance));
		if (elements.equals(sortedElements)) {
			return Stream.empty();
		}
		return JqwikStreamSupport.concat(
			fullSort(sortedElements),
			pairwiseSort(elements)
		);
	}

	private Stream<Shrinkable<List<T>>> fullSort(List<Shrinkable<T>> sortedElements) {
		return Stream.of(createShrinkable(sortedElements));
	}

	// TODO: Remove duplication with ShrinkableContainer.pairwiseSort
	private Stream<Shrinkable<List<T>>> pairwiseSort(List<Shrinkable<T>> elements) {
		return Combinatorics
				   .distinctPairs(elements.size())
				   .map(pair -> {
					   int firstIndex = Math.min(pair.get1(), pair.get2());
					   int secondIndex = Math.max(pair.get1(), pair.get2());
					   Shrinkable<T> first = elements.get(firstIndex);
					   Shrinkable<T> second = elements.get(secondIndex);
					   return Tuple.of(firstIndex, first, secondIndex, second);
				   })
				   .filter(quadruple -> quadruple.get2().compareTo(quadruple.get4()) > 0)
				   .map(quadruple -> {
					   List<Shrinkable<T>> pairSwap = new ArrayList<>(elements);
					   pairSwap.set(quadruple.get1(), quadruple.get4());
					   pairSwap.set(quadruple.get3(), quadruple.get2());
					   return createShrinkable(pairSwap);
				   });
	}

	private CollectShrinkable<T> createShrinkable(List<Shrinkable<T>> pairSwap) {
		return new CollectShrinkable<>(pairSwap, until);
	}

	private Optional<List<Shrinkable<T>>> collectElements(int replaceIndex, Shrinkable<T> shrunkElement) {
		List<Shrinkable<T>> newElements = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++) {
			if (i == replaceIndex) {
				newElements.add(shrunkElement);
			} else {
				newElements.add(elements.get(i));
			}
			if (until.test(createValue(newElements))) {
				return Optional.of(newElements);
			}
		}
		return Optional.empty();
	}

	@Override
	public ShrinkingDistance distance() {
		return ShrinkingDistance.forCollection(elements);
	}
}
