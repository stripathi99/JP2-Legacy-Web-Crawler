package com.udacity.webcrawler.profiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = delegate;
    this.state = state;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object result;
    boolean flag = false;

    for (Annotation annotation : method.getAnnotations()) {
      if (annotation instanceof Profiled) {
        flag = true;
        break;
      }
    }

    if (flag) {
      Instant startTime = clock.instant();
      try {
        result = method.invoke(delegate, args);
      } catch (IllegalAccessException illegalAccessException) {
        throw new RuntimeException(illegalAccessException);
      } catch (InvocationTargetException invocationTargetException) {
        throw invocationTargetException.getTargetException();
      } finally {
        Duration duration = Duration.between(startTime, clock.instant());
        state.record(delegate.getClass(), method, duration);
      }
    } else {
      try {
        result = method.invoke(delegate, args);
      } catch (IllegalAccessException illegalAccessException) {
        throw new RuntimeException(illegalAccessException);
      } catch (InvocationTargetException invocationTargetException) {
        throw invocationTargetException.getTargetException();
      }
    }
    return result;
  }
}
