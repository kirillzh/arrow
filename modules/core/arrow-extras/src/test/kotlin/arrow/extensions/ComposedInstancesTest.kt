package arrow.extensions

import arrow.Kind
import arrow.Kind2
import arrow.core.*
import arrow.data.*
import arrow.core.extensions.function1.contravariant.contravariant
import arrow.core.extensions.id.monad.monad
import arrow.data.extensions.listk.applicative.applicative
import arrow.data.extensions.listk.monoidK.monoidK
import arrow.data.extensions.listk.semigroupK.semigroupK
import arrow.data.extensions.nonemptylist.applicative.applicative
import arrow.data.extensions.nonemptylist.foldable.foldable
import arrow.data.extensions.nonemptylist.functor.functor
import arrow.data.extensions.nonemptylist.monad.monad
import arrow.data.extensions.nonemptylist.traverse.traverse
import arrow.core.extensions.option.applicative.applicative
import arrow.core.extensions.option.foldable.foldable
import arrow.core.extensions.option.functor.functor
import arrow.core.extensions.option.traverse.traverse
import arrow.core.extensions.tuple2.bifunctor.bifunctor
import arrow.mtl.extensions.ComposedFunctorFilter
import arrow.mtl.extensions.optiont.functorFilter.functorFilter
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.*
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

typealias OptionTNel = Kind<OptionTPartialOf<ForNonEmptyList>, Int>

@RunWith(KotlinTestRunner::class)
class ComposedInstancesTest : UnitSpec() {
  init {
    val EQ_OPTION_NEL: Eq<NestedType<ForOption, ForNonEmptyList, Int>> = Eq { a, b ->
      a.unnest().fix() == b.unnest().fix()
    }

    val EQ_LK_OPTION: Eq<NestedType<ForListK, ForOption, Int>> = Eq { a, b ->
      a.unnest().fix() == b.unnest().fix()
    }

    val EQ_OPTIONT_ID_NEL: Eq<NestedType<OptionTPartialOf<ForId>, OptionTPartialOf<ForNonEmptyList>, Int>> =
      Eq { a, b ->
        a.unnest().value().value().fold(
          { b.unnest().value().value().isEmpty() },
          { optionA: OptionTNel ->
            b.unnest().value().value().fix().fold(
              { false },
              { it.value() == optionA.value() })
          })
      }

    val EQ_OPTION_FN1: Eq<NestedType<ForOption, Conested<ForFunction1, Int>, Int>> = Eq { a, b ->
      a.unnest().fix().fold(
          { b.unnest().fix().isEmpty() },
          { fnA ->
            b.unnest().fix().fold(
                { false },
                { it.counnest().invoke(1) == fnA.counnest().invoke(1) }
            )
          }
      )
    }

    val EQ_TUPLE2: Eq<Kind2<Nested<ForTuple2, ForTuple2>, Int, Int>> = Eq { a, b ->
      a.biunnest().fix() == b.biunnest().fix()
    }

    val cf: (Int) -> Kind<Nested<ForOption, ForNonEmptyList>, Int> = { Some(it.nel()).nest() }

    val cf2: (Int) -> Kind<Nested<ForOption, Conested<ForFunction1, Int>>, Int> = { x: Int ->
      Some({ y: Int -> x + y }.k().conest()).nest()
    }

    val bifunctorCf: (Int) -> Kind2<Nested<ForTuple2, ForTuple2>, Int, Int> = { Tuple2(Tuple2(it, it), Tuple2(it, it)).binest() }

    testLaws(
      InvariantLaws.laws(ComposedInvariantCovariant(Option.functor(), NonEmptyList.functor()), cf, EQ_OPTION_NEL)
    )

    testLaws(
      InvariantLaws.laws(ComposedInvariantContravariant(Option.functor(), Function1.contravariant<Int>()), cf2, EQ_OPTION_FN1)
    )

    testLaws(
      FunctorLaws.laws(ComposedFunctor(Option.functor(), NonEmptyList.functor()), cf, EQ_OPTION_NEL),
      FunctorFilterLaws.laws(ComposedFunctorFilter(OptionT.functorFilter(Id.monad()), OptionT.functorFilter(NonEmptyList.monad())), { OptionT.just(Id.monad(), OptionT.just(NonEmptyList.monad(), it)).nest() }, EQ_OPTIONT_ID_NEL),
      ApplicativeLaws.laws(ComposedApplicative(Option.applicative(), NonEmptyList.applicative()), EQ_OPTION_NEL),
      FoldableLaws.laws(ComposedFoldable(Option.foldable(), NonEmptyList.foldable()), cf, Eq.any()),
      TraverseLaws.laws(ComposedTraverse(Option.traverse(), NonEmptyList.traverse(), NonEmptyList.applicative()), ComposedFunctor.invoke(Option.functor(), NonEmptyList.functor()), cf, EQ_OPTION_NEL),
      SemigroupKLaws.laws(ComposedSemigroupK<ForListK, ForOption>(ListK.semigroupK()), ComposedApplicative(ListK.applicative(), Option.applicative()), EQ_LK_OPTION),
      MonoidKLaws.laws(ComposedMonoidK<ForListK, ForOption>(ListK.monoidK()), ComposedApplicative(ListK.applicative(), Option.applicative()), EQ_LK_OPTION),
      BifunctorLaws.laws(ComposedBifunctor(Tuple2.bifunctor(), Tuple2.bifunctor()), bifunctorCf, EQ_TUPLE2)
    )
  }
}
