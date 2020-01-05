package io.kotest.spring

import io.kotest.SpecInterface
import io.kotest.core.TestCase
import io.kotest.core.TestResult
import io.kotest.core.specs.SpecBuilder
import io.kotest.extensions.ConstructorExtension
import io.kotest.extensions.TestListener
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR
import org.springframework.test.context.TestContextManager
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object SpringListener : TestListener {

   private val logger = LoggerFactory.getLogger(SpringListener::class.java)

   // Each Spec needs its own context. However, this listener is a singleton, so we need
   // to keep this map to separate those contexts instead of making this class non-singleton, thus
   // breaking client code
   private val testContexts = mutableMapOf<SpecInterface, TestContextManager>()

   override fun beforeSpec(spec: SpecInterface) {
      testContexts[spec] = TestContextManager(spec.javaClass)
      spec.testContext.beforeTestClass()
      spec.testContext.prepareTestInstance(spec)
   }

   override fun beforeTest(testCase: TestCase) {
      testCase.spec.testContext.beforeTestMethod(testCase.spec, testCase.spec.method)
      testCase.spec.testContext.beforeTestExecution(testCase.spec, testCase.spec.method)

   }

   override fun afterTest(testCase: TestCase, result: TestResult) {
      testCase.spec.testContext.afterTestMethod(testCase.spec, testCase.spec.method, null as Throwable?)
      testCase.spec.testContext.afterTestExecution(testCase.spec, testCase.spec.method, null as Throwable?)
   }

   override fun afterSpec(spec: SpecInterface) {
      spec.testContext.afterTestClass()
   }

   private val SpecInterface.testContext: TestContextManager
      get() = testContexts.getValue(this)

   // Check https://github.com/kotlintest/kotlintest/issues/950#issuecomment-524127221
   // for a in-depth explanation. Too much to write here
   private val SpecInterface.method: Method
      get() {
         val klass = this::class.java

         return if (Modifier.isFinal(klass.modifiers)) {
            logger.warn("Using SpringListener on a final class. If any Spring annotation fails to work, try making this class open.")
            this@SpringListener::class.java.getMethod("afterSpec", SpecInterface::class.java)
         } else {
            val fakeSpec = ByteBuddy()
               .subclass(klass)
               .defineMethod("kotestDummyMethod", String::class.java, Visibility.PUBLIC)
               .intercept(FixedValue.value("Foo"))
               .make()
               .load(this::class.java.classLoader, ClassLoadingStrategy.Default.CHILD_FIRST)
               .loaded

            fakeSpec.getMethod("kotestDummyMethod")
         }
      }
}

object SpringAutowireConstructorExtension : ConstructorExtension {
   override fun <T : SpecBuilder> instantiate(clazz: KClass<T>): SpecBuilder? {
      // we only instantiate via spring if there's actually parameters in the constructor
      // otherwise there's nothing to inject there
      val constructor = clazz.primaryConstructor
      return if (constructor == null || constructor.parameters.isEmpty()) {
         null
      } else {
         val manager = TestContextManager(clazz.java)
         val ac = manager.testContext.applicationContext
         ac.autowireCapableBeanFactory.autowire(clazz.java, AUTOWIRE_CONSTRUCTOR, true) as SpecBuilder
      }
   }
}
