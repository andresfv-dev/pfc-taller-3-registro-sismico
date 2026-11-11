# Informe de corrección

Fundamentos de Programación Funcional y Concurrente.
Taller 3 — Consultas sobre un registro sísmico.

## Cómo se argumenta la corrección en este taller

Igual que en el ejemplo del factorial, para cada función $P_f$ se demuestra
que siempre devuelve lo que pide su especificación $f$:

```math
\forall a \in A : P_f(a) == f(a)
```

Las funciones del punto 1 no son recursivas a mano: recorren una lista con una
expresión `for`. Pero las listas son un conjunto **definido recursivamente**:

- $\text{List}()$ es una lista (valor básico).
- Si $s$ es un elemento y $l$ es una lista, $s :: l$ es una lista (valor
  construido a partir de otro).

Entonces se usa **inducción estructural sobre la lista que recorre el `for`**:

- **Caso base:** la lista vacía, $\text{List}()$.
- **Caso de inducción:** la lista $s :: r'$ (un sismo $s$ al frente de una
  lista $r'$ más corta). La hipótesis de inducción (HI) es que el programa es
  correcto para $r'$.

### Cómo se evalúa un `for` (modelo de sustitución)

El compilador traduce un `for` con un generador y un filtro así:

```scala
for { x <- l; if (c(x)) } yield g(x)   ==   l.withFilter(x => c(x)).map(x => g(x))
```

Y `withFilter` (filtrar) y `map` sobre listas cumplen estas reglas, que son
las que se usan en las demostraciones:

```math
\text{List}().\text{withFilter}(c).\text{map}(g) \rightarrow \text{List}()
```

```math
(s :: r').\text{withFilter}(c).\text{map}(g) \rightarrow
\begin{cases}
g(s) :: r'.\text{withFilter}(c).\text{map}(g) & \text{si } c(s) \\
r'.\text{withFilter}(c).\text{map}(g) & \text{si } \neg c(s)
\end{cases}
```

Es decir: con la lista vacía el `for` da la lista vacía, y con $s :: r'$
revisa primero a $s$ y después sigue con el resto **en el mismo orden**.

Notación: para un sismo $s$, $\text{reg}(s)$ es `s.region` y
$\text{mag}(s)$ es `s.magnitud`.

---

## Punto 1: consultas con expresiones `for`

### 1.1 `fuertes`

```scala
def fuertes(r: Registro, umbral: Double): List[String] = {
  for {
    sismo <- r
    if (sismo.magnitud > umbral)
  } yield sismo.region
}
```

**Especificación.** Sea $u$ el umbral. Para
$r = \text{List}(s_1, s_2, \ldots, s_n)$, $f(r, u)$ es la lista de las regiones
de los sismos con magnitud **estrictamente** mayor que $u$, en el orden del
registro y con repetidos:

```math
f(\text{List}(s_1, \ldots, s_n), u) = \text{List}(\text{reg}(s_i) \mid 1 \le i \le n,\ \text{mag}(s_i) > u) \quad \text{(con } i \text{ creciente)}
```

De esa definición salen directamente sus dos propiedades recursivas:

```math
f(\text{List}(), u) = \text{List}()
```

```math
f(s :: r', u) =
\begin{cases}
\text{reg}(s) :: f(r', u) & \text{si } \text{mag}(s) > u \\
f(r', u) & \text{si } \text{mag}(s) \le u
\end{cases}
```

Vamos a demostrar que
$\forall n \in \mathbb{N} : P_f(\text{List}(s_1, \ldots, s_n), u) == f(\text{List}(s_1, \ldots, s_n), u)$.

**Caso base:** $n = 0$, $r = \text{List}()$

```math
P_f(\text{List}(), u) \rightarrow \text{List}().\text{withFilter}(\text{mag} > u).\text{map}(\text{reg}) \rightarrow \text{List}()
```

Por otro lado, $f(\text{List}(), u) = \text{List}()$ (no hay sismos que superen
el umbral). Entonces $P_f(\text{List}(), u) == f(\text{List}(), u)$.

**Caso de inducción:** $n = k + 1$, $k \ge 0$, con $r = s :: r'$ y $r'$ de
longitud $k$. Hay que demostrar:

```math
P_f(r', u) == f(r', u) \rightarrow P_f(s :: r', u) == f(s :: r', u)
```

Por el modelo de sustitución hay dos casos:

- Si $\text{mag}(s) > u$ (el filtro deja pasar a $s$):

```math
P_f(s :: r', u) \rightarrow \text{reg}(s) :: P_f(r', u) \overset{HI}{=} \text{reg}(s) :: f(r', u) = f(s :: r', u)
```

- Si $\text{mag}(s) \le u$ (el filtro no deja pasar a $s$):

```math
P_f(s :: r', u) \rightarrow P_f(r', u) \overset{HI}{=} f(r', u) = f(s :: r', u)
```

En los dos casos $P_f(s :: r', u) == f(s :: r', u)$.

Concluimos por inducción que $\forall r, u : P_f(r, u) == f(r, u)$.

Observaciones que la prueba confirma:

- El orden se conserva, porque $\text{reg}(s)$ se pone **delante** del
  resultado del resto.
- Los repetidos se conservan, porque ningún paso elimina elementos ya puestos.
- El umbral es estricto: con $\text{mag}(s) = u$ se cae en el segundo caso y
  $s$ no aparece (por eso `fuertes(registro, 6.0)` no contiene `"Choco"`).

---

### 1.2 `enRegion`

```scala
def enRegion(r: Registro, region: String): List[Double] = {
  for {
    sismo <- r
    if (sismo.region == region)
  } yield sismo.magnitud
}
```

**Especificación.** Sea $g$ la región pedida:

```math
f(\text{List}(s_1, \ldots, s_n), g) = \text{List}(\text{mag}(s_i) \mid 1 \le i \le n,\ \text{reg}(s_i) = g) \quad \text{(con } i \text{ creciente)}
```

Con sus propiedades recursivas:

```math
f(\text{List}(), g) = \text{List}()
```

```math
f(s :: r', g) =
\begin{cases}
\text{mag}(s) :: f(r', g) & \text{si } \text{reg}(s) = g \\
f(r', g) & \text{si } \text{reg}(s) \ne g
\end{cases}
```

**Caso base:** $r = \text{List}()$

```math
P_f(\text{List}(), g) \rightarrow \text{List}().\text{withFilter}(\text{reg} = g).\text{map}(\text{mag}) \rightarrow \text{List}()
```

Y $f(\text{List}(), g) = \text{List}()$. Entonces son iguales.

**Caso de inducción:** $r = s :: r'$. Hay que demostrar:

```math
P_f(r', g) == f(r', g) \rightarrow P_f(s :: r', g) == f(s :: r', g)
```

- Si $\text{reg}(s) = g$:

```math
P_f(s :: r', g) \rightarrow \text{mag}(s) :: P_f(r', g) \overset{HI}{=} \text{mag}(s) :: f(r', g) = f(s :: r', g)
```

- Si $\text{reg}(s) \ne g$:

```math
P_f(s :: r', g) \rightarrow P_f(r', g) \overset{HI}{=} f(r', g) = f(s :: r', g)
```

Concluimos por inducción que $\forall r, g : P_f(r, g) == f(r, g)$.

Consecuencia que se usa en `paresCercanos`: un número $m$ está en
`enRegion(r, g)` **si y solo si** hay un sismo de la región $g$ con magnitud
$m$. En particular, si $g$ no aparece en $r$ (por ejemplo `"Amazonas"`), el
resultado es $\text{List}()$.

---

### 1.3 `paresCercanos`

```scala
def paresCercanos(r: Registro, dif: Double): List[(String, String)] = {
  val regiones = r.map(_.region).distinct
  for {
    region1 :: resto <- regiones.tails.toList
    region2 <- resto
    if ((for {
      magnitud1 <- enRegion(r, region1)
      magnitud2 <- enRegion(r, region2)
      if (math.abs(magnitud1 - magnitud2) <= dif)
    } yield (magnitud1, magnitud2)) != List())
  } yield (region1, region2)
}
```

**Especificación.** Sea $d$ = `dif` y sea
$R = \text{List}(g_1, g_2, \ldots, g_m)$ la lista de regiones distintas de $r$
en el orden en que aparecen por primera vez. Decimos que dos regiones están
**cerca** si:

```math
\text{cerca}(a, b) \equiv \exists\, s, t \in r : \text{reg}(s) = a \land \text{reg}(t) = b \land |\text{mag}(s) - \text{mag}(t)| \le d
```

Entonces $f(r, d)$ es la lista de parejas $(g_i, g_j)$ con $i < j$ que están
cerca, ordenadas primero por $i$ y luego por $j$:

```math
f(r, d) = \text{List}((g_i, g_j) \mid 1 \le i < j \le m,\ \text{cerca}(g_i, g_j))
```

Como $i < j$ y las $g$ son distintas, **nunca** sale una región con ella misma
y **cada pareja sale una sola vez**.

La demostración se divide en tres partes (lemas).

#### Lema 1: `regiones` es la lista $R$

`r.map(_.region)` cambia cada sismo por su región, conservando el orden, y
`.distinct` quita las repeticiones dejando la primera aparición. Por lo tanto
`regiones` $= R = \text{List}(g_1, \ldots, g_m)$, con $g_i \ne g_j$ si
$i \ne j$.

#### Lema 2: el `for` interno decide correctamente si dos regiones están cerca

Sea $L_1 = $ `enRegion(r, a)` y $L_2 = $ `enRegion(r, b)`, y sea

```math
C(L_1, L_2) = \text{for } \{ m_1 \leftarrow L_1;\ m_2 \leftarrow L_2;\ \text{if } |m_1 - m_2| \le d \} \text{ yield } (m_1, m_2)
```

Se demuestra por inducción sobre $L_1$ que:

```math
C(L_1, L_2) \ne \text{List}() \iff \exists\, m_1 \in L_1,\ m_2 \in L_2 : |m_1 - m_2| \le d
```

Con dos generadores, el `for` se traduce a
$L_1.\text{flatMap}(m_1 \Rightarrow L_2.\text{withFilter}(\ldots).\text{map}(\ldots))$,
así que:

```math
C(\text{List}(), L_2) \rightarrow \text{List}()
```

```math
C(m :: L_1', L_2) \rightarrow \text{List}((m, m_2) \mid m_2 \in L_2,\ |m - m_2| \le d) \mathbin{+\!\!+} C(L_1', L_2)
```

**Caso base:** $L_1 = \text{List}()$. $C$ da $\text{List}()$, y no existe
ningún $m_1 \in \text{List}()$. Los dos lados de $\iff$ son falsos.

**Caso de inducción:** $L_1 = m :: L_1'$, con HI para $L_1'$. La concatenación
$A \mathbin{+\!\!+} B$ es no vacía si y solo si $A$ o $B$ lo es:

- $A \ne \text{List}()$ si y solo si existe $m_2 \in L_2$ con
  $|m - m_2| \le d$ (es un `for` de un generador y un filtro, como en 1.1).
- $B = C(L_1', L_2) \ne \text{List}()$ si y solo si, por HI, existe
  $m_1 \in L_1'$ y $m_2 \in L_2$ con $|m_1 - m_2| \le d$.

Juntando los dos: $C(m :: L_1', L_2) \ne \text{List}()$ si y solo si existe
$m_1 \in m :: L_1'$ y $m_2 \in L_2$ cercanos. ∎

Por la corrección de `enRegion` (1.2), $L_1$ son exactamente las magnitudes de
$a$ y $L_2$ las de $b$. Entonces la condición del filtro
`(for {...} yield ...) != List()` es verdadera **si y solo si**
$\text{cerca}(a, b)$.

#### Lema 3: el `for` externo genera las parejas $(g_i, g_j)$ con $i < j$

Para una lista de regiones $R$, sea

```math
T(R) = \text{for } \{ a :: \text{resto} \leftarrow R.\text{tails.toList};\ b \leftarrow \text{resto};\ \text{if cerca}(a, b) \} \text{ yield } (a, b)
```

`tails` cumple:

```math
\text{List}().\text{tails} = \text{List}(\text{List}()) \qquad (g :: R').\text{tails} = (g :: R') :: R'.\text{tails}
```

El patrón `a :: resto` **no** encaja con la lista vacía, así que esa cola se
salta (el `for` la filtra). Por eso:

```math
T(\text{List}()) \rightarrow \text{List}()
```

```math
T(g :: R') \rightarrow \text{List}((g, b) \mid b \in R',\ \text{cerca}(g, b)) \mathbin{+\!\!+} T(R')
```

Y la especificación cumple la misma relación, separando las parejas cuyo
primer elemento es $g_1 = g$ de las demás:

```math
f(g :: R') = \text{List}((g, b) \mid b \in R',\ \text{cerca}(g, b)) \mathbin{+\!\!+} f(R')
```

Se demuestra $T(R) == f(R)$ por inducción sobre la longitud de $R$:

**Caso base:** $R = \text{List}()$. $T(\text{List}()) = \text{List}()$, y sin
regiones no hay parejas: $f(\text{List}()) = \text{List}()$.

**Caso de inducción:** $R = g :: R'$, con HI: $T(R') == f(R')$.

```math
T(g :: R') \rightarrow \text{List}((g, b) \mid b \in R',\ \text{cerca}(g, b)) \mathbin{+\!\!+} T(R')
\overset{HI}{=} \text{List}((g, b) \mid b \in R',\ \text{cerca}(g, b)) \mathbin{+\!\!+} f(R') = f(g :: R')
```

∎

#### Conclusión para `paresCercanos`

Por el Lema 1, el `for` recorre $R$; por el Lema 2, su filtro es exactamente
$\text{cerca}(a, b)$; y por el Lema 3, el resultado es la lista de parejas
$(g_i, g_j)$ con $i < j$ que están cerca, en orden. Entonces:

```math
\forall r, d : P_f(r, d) == f(r, d)
```

**Terminación:** el registro es una lista finita, así que `regiones`, sus
`tails` y cada `enRegion` son finitas; todos los generadores terminan.

**Verificación con los datos de ejemplo** ($d = 0.6$):
$R = \text{List}(\text{Valle}, \text{Santander}, \text{Choco})$.

| Pareja | Magnitudes | ¿Hay una diferencia $\le 0.6$? | ¿Sale? |
|---|---|---|---|
| (Valle, Santander) | (4.2, 3.8, 5.3) vs (5.1, 4.7) | sí: 5.3 − 5.1 = 0.2 | sí |
| (Valle, Choco) | (4.2, 3.8, 5.3) vs (6.0, 3.1) | no: la menor es 0.7 | no |
| (Santander, Choco) | (5.1, 4.7) vs (6.0, 3.1) | no: la menor es 0.9 | no |

Resultado: $\text{List}((\text{Valle}, \text{Santander}))$, que es lo que
espera la prueba.
