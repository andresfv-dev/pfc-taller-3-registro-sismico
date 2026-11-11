package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RegistroSismicoPuntos45Test extends AnyFunSuite {

  val s = new RegistroSismico()
  import s._

  // Punto 4: pruebas propias --------------------------------------------------

  test("replicas: el redondeo no se acumula en la recurrencia") {
    assert(replicas(1.23).take(5).toList == List(1.23, 0.98, 0.79, 0.63, 0.50))
  }

  test("replicas: conserva la precisión inicial para calcular el siguiente valor") {
    assert(replicas(1.234).take(2).toList == List(1.23, 0.99))
  }

  test("replicas: el cero sigue produciendo un prefijo de 257 elementos") {
    assert(replicas(0.0).take(257).toList == List.fill(257)(0.0))
  }

  test("replicas: tomar un prefijo no evalúa el siguiente elemento transformado") {
    val valores = replicas(1.23).map { x =>
      assert(x != 0.63)
      x
    }
    assert(valores.take(3).toList == List(1.23, 0.98, 0.79))
  }

  test("replicasSensibles: compara el mínimo con los valores redondeados") {
    assert(replicasSensibles(1.23, 0.788) == List(1.23, 0.98, 0.79))
  }

  test("replicasSensibles: termina antes de una réplica igual al mínimo") {
    assert(replicasSensibles(4.5, 2.88) == List(4.5, 3.6))
  }

  test("replicasSensibles: con mínimo cero termina al redondear a cero") {
    assert(replicasSensibles(0.01, 0.0) == List(0.01, 0.01, 0.01, 0.01))
  }

  test("replicasSensibles: rechaza la primera si al redondear iguala el mínimo") {
    assert(replicasSensibles(1.234, 1.23) == List())
  }

  // Punto 5: pruebas propias --------------------------------------------------

  test("informe: una región con un sismo cuenta también la magnitud inicial") {
    val r = List(Sismo(Fecha(2026, 1), "Cauca", 2.75, 12))
    assert(informe(r, 2.0) == Map("Cauca" -> Resumen(1, 2.75, 2)))
  }

  test("informe: agrupa regiones intercaladas y usa una máxima no inicial") {
    val r = List(
      Sismo(Fecha(2026, 1), "Cauca", 1.5, 12),
      Sismo(Fecha(2026, 2), "Huila", 3.0, 20),
      Sismo(Fecha(2026, 3), "Cauca", 4.0, 8),
      Sismo(Fecha(2026, 4), "Huila", 2.5, 15))
    assert(informe(r, 2.0) ==
      Map("Cauca" -> Resumen(2, 4.0, 4), "Huila" -> Resumen(2, 3.0, 2)))
  }

  test("informe: duplicar un sismo aumenta solo el conteo") {
    val evento = Sismo(Fecha(2026, 1), "Meta", 2.5, 10)
    assert(informe(List(evento), 1.5) == Map("Meta" -> Resumen(1, 2.5, 3)))
    assert(informe(List(evento, evento), 1.5) == Map("Meta" -> Resumen(2, 2.5, 3)))
  }

  test("informe: conserva regiones cuya máxima no supera el mínimo") {
    val r = List(
      Sismo(Fecha(2026, 1), "Meta", 1.5, 10),
      Sismo(Fecha(2026, 2), "Cauca", 2.0, 12))
    assert(informe(r, 2.0) ==
      Map("Meta" -> Resumen(1, 1.5, 0), "Cauca" -> Resumen(1, 2.0, 0)))
  }

  test("informe: una permutación conserva el mapa completo") {
    val a = Sismo(Fecha(2026, 1), "Meta", 3.5, 10)
    val b = Sismo(Fecha(2026, 2), "Huila", 2.0, 20)
    val c = Sismo(Fecha(2026, 3), "Meta", 1.0, 8)
    val esperado = Map("Meta" -> Resumen(2, 3.5, 3), "Huila" -> Resumen(1, 2.0, 0))
    assert(informe(List(a, b, c), 2.0) == esperado)
    assert(informe(List(c, a, b), 2.0) == esperado)
  }

  test("informe: guarda la máxima exacta y redondea solamente sus réplicas") {
    val r = List(Sismo(Fecha(2026, 1), "Cauca", 1.234, 12))
    assert(informe(r, 1.23) == Map("Cauca" -> Resumen(1, 1.234, 0)))
  }
}
