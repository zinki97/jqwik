package net.jqwik.support;

import net.jqwik.api.*;
import org.assertj.core.api.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

@Group
class JqwikReflectionSupportTests {

	@Group
	class NewInstanceWithDefaultConstructor {

		@Example
		boolean staticClass() {
			return JqwikReflectionSupport.newInstanceWithDefaultConstructor(Outer.class) instanceof Outer;
		}

		@Example
		boolean innerClassWithoutConstructor() {
			return JqwikReflectionSupport.newInstanceWithDefaultConstructor(Outer.Inner.class) instanceof Outer.Inner;
		}

		@Example
		boolean innerClassWithDefaultConstructor() {
			return JqwikReflectionSupport
					.newInstanceWithDefaultConstructor(Outer.InnerWithConstructor.class) instanceof Outer.InnerWithConstructor;
		}

		@Example
		boolean staticClassWithDefaultConstructor() {
			return JqwikReflectionSupport.newInstanceWithDefaultConstructor(OuterWithConstructor.class) instanceof OuterWithConstructor;
		}

	}

	@Group
	class GetMethodParameters {

		@Example
		void simpleParameters() throws NoSuchMethodException {
			class ClassWithMethod {
				public void method(@ForAll String param1, List<Integer> param2) {

				}
			}

			Method method = ClassWithMethod.class.getMethod("method", String.class, List.class);
			MethodParameter[] parameters = JqwikReflectionSupport.getMethodParameters(method, ClassWithMethod.class);

			MethodParameter param1 = parameters[0];
			Assertions.assertThat(param1.getType()).isEqualTo(String.class);
			Assertions.assertThat(param1.isAnnotatedParameterized()).isFalse();
			Assertions.assertThat(param1.getAnnotatedType()).isNull();
			Assertions.assertThat(param1.getParameterizedType()).isEqualTo(String.class);
			Assertions.assertThat(param1.findAnnotation(ForAll.class)).isPresent();

			MethodParameter param2 = parameters[1];
			Assertions.assertThat(param2.getType()).isEqualTo(List.class);
			Assertions.assertThat(param2.isAnnotatedParameterized()).isTrue();
			Assertions.assertThat(param2.getAnnotatedType().getAnnotatedActualTypeArguments()[0].getType()).isEqualTo(Integer.class);
			Assertions.assertThat(param2.getParameterizedType()).isEqualTo(param2.getAnnotatedType().getType());
			Assertions.assertThat(param2.findAllAnnotations()).isEmpty();
		}

		@Example
		void typeVariableParameter() throws NoSuchMethodException {
			class ClassWithTypeVariableMethod {
				public <T> void method(T param1) {

				}
			}

			Method method = ClassWithTypeVariableMethod.class.getMethod("method", Object.class);
			MethodParameter[] parameters = JqwikReflectionSupport.getMethodParameters(method, ClassWithTypeVariableMethod.class);

			MethodParameter param1 = parameters[0];
			Assertions.assertThat(param1.getType()).isEqualTo(Object.class);
			Assertions.assertThat(param1.isAnnotatedParameterized()).isFalse();
			Assertions.assertThat(param1.getAnnotatedType()).isNull();
			Assertions.assertThat(param1.getParameterizedType()).isInstanceOf(TypeVariable.class);
		}

		@Example
		void typeVariableParameterResolvedInSubclass() throws NoSuchMethodException {
			class ClassWithTypeVariable<T> {
				public void method(@ForAll T param1) {

				}
			}

			class ClassWithString extends ClassWithTypeVariable<String> {

			}

			Method method = ClassWithString.class.getMethod("method", Object.class);
			MethodParameter[] parameters = JqwikReflectionSupport.getMethodParameters(method, ClassWithString.class);

			MethodParameter param1 = parameters[0];
			Assertions.assertThat(param1.getType()).isEqualTo(String.class);
			Assertions.assertThat(param1.getParameterizedType()).isEqualTo(String.class);
			Assertions.assertThat(param1.findAnnotation(ForAll.class)).isPresent();

			// TODO: if ClassWithTypeVariable<@MyAnnotation String> has annotations
			//       they should be available but they are not:
			Assertions.assertThat(param1.isAnnotatedParameterized()).isFalse();
			Assertions.assertThat(param1.getAnnotatedType()).isNull();
		}

		//@Example //TODO: Does not work yet
		void parameterWithGenericTypeResolvedInSubclass() throws NoSuchMethodException {
			class ClassWithGenericType<T> {
				public<S> void method(@ForAll List<T> param1, @ForAll List<S> param2) {

				}
			}

			class ClassWithListOfString extends ClassWithGenericType<String> {

			}

			Method method = ClassWithListOfString.class.getMethod("method", List.class, List.class);
			MethodParameter[] parameters = JqwikReflectionSupport.getMethodParameters(method, ClassWithListOfString.class);

			MethodParameter param1 = parameters[0];
			Assertions.assertThat(param1.getType()).isEqualTo(List.class);
			Assertions.assertThat(param1.isAnnotatedParameterized()).isFalse();
			Assertions.assertThat(param1.getAnnotatedType()).isNull();
			Assertions.assertThat(param1.getParameterizedType()).isEqualTo(String.class);
			Assertions.assertThat(param1.findAnnotation(ForAll.class)).isPresent();

		}
	}

	@Example
	void streamInnerInstances() {
		Outer outer = new Outer();
		Outer.Inner inner = outer.createInner();

		Stream<Object> instances = JqwikReflectionSupport.streamInnerInstances(inner);

		Assertions.assertThat(instances).containsExactly(inner, outer);
	}

	private static class Outer {

		Inner createInner() {
			return new Inner();
		}

		class Inner {

		}

		class InnerWithConstructor {
			private final String aString;
			public InnerWithConstructor() {
				this.aString = "hallo";
			}
		}
	}

	private static class OuterWithConstructor {

	}
}
