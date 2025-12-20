package org.aulune.commons
package instances


import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror


/** Makes derivation easy.
 *  @tparam TC type class to derive.
 */
trait WithDerivation[TC[_]]:
  protected final case class ProductTypeElement[A, B](
      label: String,
      typeclass: TC[B],
      getValue: A => B,
      idx: Int,
  )
  protected final case class ProductType[A](
      label: String,
      elements: List[ProductTypeElement[A, _]],
      fromElements: List[Any] => A,
  )

  protected final case class SumTypeElement[A, B](
      label: String,
      typeclass: TC[B],
      idx: Int,
      cast: A => B,
  )
  protected final case class SumType[A](
      label: String,
      elements: List[SumTypeElement[A, _]],
      getElement: A => SumTypeElement[A, ?],
  )

  private inline def getInstances[A <: Tuple]: List[TC[Any]] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[TC[t]].asInstanceOf[TC[Any]] :: getInstances[ts]

  private inline def getElemLabels[A <: Tuple]: List[String] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => constValue[t].toString :: getElemLabels[ts]

  /** Derives product types.
   *  @param productType product type to derive.
   *  @tparam A product type generic argument.
   *  @return derived type class.
   */
  def deriveProduct[A](productType: ProductType[A]): TC[A]

  /** Derives sum types.
   *  @param sumType sum type to derive.
   *  @tparam A sum type generic argument.
   *  @return derived type class.
   */
  def deriveSum[A](sumType: SumType[A]): TC[A]

  /** Derives type class for [[A]].
   *  @param m mirror of [[A]].
   *  @tparam A type of generic argument.
   */
  inline given derived[A](using
      m: Mirror.Of[A],
  ): TC[A] =
    val label = constValue[m.MirroredLabel]
    val elemInstances = getInstances[m.MirroredElemTypes]
    val elemLabels = getElemLabels[m.MirroredElemLabels]

    inline m match
      case s: Mirror.SumOf[A] =>
        val elements = elemInstances.zip(elemLabels).zipWithIndex.map {
          case ((inst, lbl), idx) =>
            SumTypeElement[A, Any](lbl, inst, idx, identity)
        }
        val getElement = (a: A) => elements(s.ordinal(a))
        deriveSum(SumType[A](label, elements, getElement))

      case p: Mirror.ProductOf[A] =>
        val caseClassElements = elemInstances
          .zip(elemLabels)
          .zipWithIndex
          .map { case ((inst, lbl), idx) =>
            ProductTypeElement[A, Any](
              lbl,
              inst,
              (x: Any) => x.asInstanceOf[Product].productElement(idx),
              idx)
          }
        val fromElements: List[Any] => A =
          elements => p.fromProduct(Tuple.fromArray(elements.toArray))
        deriveProduct(ProductType[A](label, caseClassElements, fromElements))
