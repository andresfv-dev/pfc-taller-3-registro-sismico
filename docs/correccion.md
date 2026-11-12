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

---

## Punto 2: agrupar con mapas

### Notación

Para una región $g$ y un registro $r$, sea $\text{filtro}_g(r)$ la lista de
los sismos de $r$ cuya región es $g$, en el orden del registro:

```math
\text{filtro}_g(\text{List}()) = \text{List}()
```

```math
\text{filtro}_g(s :: r') =
\begin{cases}
s :: \text{filtro}_g(r') & \text{si } \text{reg}(s) = g \\
\text{filtro}_g(r') & \text{si } \text{reg}(s) \ne g
\end{cases}
```

Y sea $\text{regs}(r) = \{\text{reg}(s) \mid s \in r\}$ el conjunto de
regiones que aparecen en $r$.

Para un mapa $M$, $\text{llaves}(M)$ es su conjunto de llaves y $M(g)$ el
valor asociado a la llave $g$. Dos mapas son iguales si tienen las mismas
llaves y el mismo valor en cada llave (el orden no importa).

---

### 2.1 `porRegion`

```scala
def porRegion(r: Registro): Map[String, List[Sismo]] = {
  r.groupBy(_.region)
}
```

**Especificación.** $f(r)$ es el mapa cuyas llaves son las regiones de $r$ y
que a cada una le asocia sus sismos, en orden:

```math
\text{llaves}(f(r)) = \text{regs}(r) \qquad \land \qquad \forall g \in \text{regs}(r) : f(r)(g) = \text{filtro}_g(r)
```

**Cómo se evalúa `groupBy`.** `groupBy` recorre la lista en orden y pone cada
elemento en el grupo de su llave. Como $s$ es el **primer** elemento de
$s :: r'$, queda **primero** en su grupo, y los demás grupos son los de $r'$:

```math
\text{List}().\text{groupBy}(h) \rightarrow \text{Map}()
```

```math
(s :: r').\text{groupBy}(h) \rightarrow M' + \big(h(s) \rightarrow s :: M'.\text{getOrElse}(h(s), \text{List}())\big)
\quad \text{donde } M' = r'.\text{groupBy}(h)
```

Vamos a demostrar por inducción sobre $r$ que $P_f(r) == f(r)$.

**Caso base:** $r = \text{List}()$

```math
P_f(\text{List}()) \rightarrow \text{List}().\text{groupBy}(\text{reg}) \rightarrow \text{Map}()
```

Por otro lado, $\text{regs}(\text{List}()) = \emptyset$, así que $f(\text{List}())$
no tiene llaves: $f(\text{List}()) = \text{Map}()$. Entonces son iguales.

**Caso de inducción:** $r = s :: r'$, con $g_0 = \text{reg}(s)$. HI:
$P_f(r') == f(r')$. Sea $M' = P_f(r')$.

```math
P_f(s :: r') \rightarrow M' + \big(g_0 \rightarrow s :: M'.\text{getOrElse}(g_0, \text{List}())\big)
```

- **Llaves:** son las de $M'$ más $g_0$. Por HI,
  $\text{llaves}(M') = \text{regs}(r')$, así que las llaves son
  $\text{regs}(r') \cup \{g_0\} = \text{regs}(s :: r')$. ✔
- **Valor en $g_0$:** si $g_0 \in \text{regs}(r')$, por HI
  $M'(g_0) = \text{filtro}_{g_0}(r')$; si no, $\text{getOrElse}$ da
  $\text{List}() = \text{filtro}_{g_0}(r')$. En los dos casos el valor es
  $s :: \text{filtro}_{g_0}(r') = \text{filtro}_{g_0}(s :: r')$. ✔
- **Valor en $g \ne g_0$:** no cambia, así que por HI es
  $\text{filtro}_g(r')$, y como $\text{reg}(s) \ne g$,
  $\text{filtro}_g(r') = \text{filtro}_g(s :: r')$. ✔

Por lo tanto $P_f(s :: r') == f(s :: r')$, y concluimos por inducción que
$\forall r : P_f(r) == f(r)$.

**Consecuencia (grupos no vacíos).** Si $g \in \text{llaves}(P_f(r))$,
entonces $g \in \text{regs}(r)$, es decir, hay al menos un sismo de $g$, y
$\text{filtro}_g(r) \ne \text{List}()$. Esto se usa en `magnitudMaxima`.

---

### Lema: `map` sobre un mapa conserva las llaves

Si $M$ es un mapa y $h$ es una función sobre los valores, entonces

```math
N = M.\text{map}\{ \text{case } (k, v) \Rightarrow (k, h(v)) \}
\implies \text{llaves}(N) = \text{llaves}(M) \ \land\ \forall k \in \text{llaves}(M) : N(k) = h(M(k))
```

Porque `map` visita cada pareja $(k, v)$ de $M$ una vez y produce $(k, h(v))$.
Como la llave $k$ no se cambia y las llaves de $M$ son distintas, no hay dos
parejas nuevas con la misma llave, así que ninguna se pierde. Se puede
demostrar por inducción sobre el número de parejas de $M$: con $0$ parejas
$N = \text{Map}()$; y al agregar una pareja $(k, v)$ a $M$ se agrega
exactamente $(k, h(v))$ a $N$.

---

### 2.2 `conteoPorRegion`

```scala
def conteoPorRegion(r: Registro): Map[String, Int] = {
  val m = porRegion(r)
  m.map { case (key, list) => (key, list.length) }
}
```

**Especificación.** A cada región de $r$, su número de sismos:

```math
\text{llaves}(f(r)) = \text{regs}(r) \qquad \land \qquad \forall g \in \text{regs}(r) : f(r)(g) = |\{ i \mid \text{reg}(s_i) = g \}|
```

**Demostración.** Por 2.1, $m = P_{\text{porRegion}}(r)$ tiene llaves
$\text{regs}(r)$ y $m(g) = \text{filtro}_g(r)$. Por el lema, con
$h(v) = v.\text{length}$:

```math
\text{llaves}(P_f(r)) = \text{regs}(r) \qquad P_f(r)(g) = \text{filtro}_g(r).\text{length}
```

Falta ver que $\text{filtro}_g(r).\text{length}$ es el número de sismos de
$g$. Por inducción sobre $r$:

- **Caso base:** $\text{filtro}_g(\text{List}()).\text{length} = 0$, y en la
  lista vacía hay $0$ sismos de $g$.
- **Caso de inducción:** si $\text{reg}(s) = g$,
  $\text{filtro}_g(s :: r').\text{length} = 1 + \text{filtro}_g(r').\text{length}$,
  que por HI es $1 +$ (sismos de $g$ en $r'$) $=$ (sismos de $g$ en $s :: r'$).
  Si $\text{reg}(s) \ne g$, la longitud es la de $\text{filtro}_g(r')$, y $s$
  tampoco cuenta. ✔

Entonces $\forall r : P_f(r) == f(r)$.

En el caso $r = \text{List}()$: $m = \text{Map}()$ y, por el lema,
$P_f(\text{List}()) = \text{Map}()$, que es lo que pide la prueba.

---

### 2.3 `magnitudMaxima`

```scala
def magnitudMaxima(r: Registro): Map[String, Double] = {
  val m = porRegion(r)
  m.map { case (k, l) => (k, l.map(_.magnitud).max) }
}
```

**Especificación.** A cada región de $r$, la mayor magnitud de sus sismos:

```math
\text{llaves}(f(r)) = \text{regs}(r) \qquad \land \qquad \forall g \in \text{regs}(r) : f(r)(g) = \max \{ \text{mag}(s) \mid s \in r,\ \text{reg}(s) = g \}
```

**Demostración.** Por 2.1 y el lema, con
$h(l) = l.\text{map}(\text{mag}).\text{max}$:

```math
\text{llaves}(P_f(r)) = \text{regs}(r) \qquad P_f(r)(g) = \text{filtro}_g(r).\text{map}(\text{mag}).\text{max}
```

- $\text{filtro}_g(r).\text{map}(\text{mag})$ es la lista de las magnitudes de
  los sismos de $g$: `map` cambia cada sismo por su magnitud, sin quitar ni
  agregar elementos. (Es la misma lista que da `enRegion(r, g)`, demostrada
  en 1.2.)
- Por la consecuencia de 2.1, $\text{filtro}_g(r)$ **no es vacía**, así que
  `max` está definido y no lanza excepción.
- `max` de una lista no vacía devuelve su mayor elemento (es la función
  $f$ del ejemplo del máximo de una lista, demostrado por inducción en el
  ejemplo del docente).

Entonces $P_f(r)(g) = \max\{\text{mag}(s) \mid s \in r,\ \text{reg}(s) = g\}$
y $\forall r : P_f(r) == f(r)$.

Las regiones ausentes no aparecen: si $g \notin \text{regs}(r)$ (por ejemplo
`"Amazonas"`), $g$ no es llave de $P_f(r)$.

---

### 2.4 El resultado de los agrupamientos no depende del orden del registro

Sea $r_2$ cualquier reordenamiento (permutación) de $r$: los mismos sismos,
cada uno el mismo número de veces, en otro orden. Se cumple:

```math
\text{conteoPorRegion}(r_2) == \text{conteoPorRegion}(r) \qquad \text{magnitudMaxima}(r_2) == \text{magnitudMaxima}(r)
```

**Demostración.** Las especificaciones de 2.2 y 2.3 dependen solo de:

1. $\text{regs}(r)$, el **conjunto** de regiones: un conjunto no cambia si se
   reordenan los elementos, así que $\text{regs}(r_2) = \text{regs}(r)$. Los
   dos mapas tienen las mismas llaves.
2. Cuántos sismos de cada región hay: reordenar no agrega ni quita sismos,
   así que el conteo de cada $g$ es el mismo.
3. El **conjunto** de magnitudes de cada región: es el mismo en $r$ y en $r_2$,
   y el máximo de un conjunto no depende del orden.

Como ya se demostró que cada función cumple su especificación, y las
especificaciones dan lo mismo para $r$ y $r_2$, los resultados son iguales.
Además, la igualdad de mapas no depende del orden de las llaves. ∎

**Sobre `porRegion`.** Con $r_2$ el mapa tiene las mismas llaves y cada grupo
tiene los mismos sismos, pero **en el orden de $r_2$** (porque
$\text{filtro}_g$ conserva el orden). Por eso `porRegion` es igual *salvo el
orden dentro de cada grupo*, mientras que el conteo y la magnitud máxima son
exactamente iguales.

**Ejemplo:** con $r_2 = $ `registro.reverse`, el grupo del Valle queda
$(5.3, 3.8, 4.2)$ en vez de $(4.2, 3.8, 5.3)$, pero su conteo sigue siendo $3$
y su máxima $5.3$. La prueba propia
`conteoPorRegion(registro.reverse) == conteoPorRegion(registro)` lo comprueba.

---

## Punto 3: consultas seguras

Las tres funciones del punto 3 no recorren el registro por su cuenta, usan
`conteoPorRegion` y `magnitudMaxima`, que ya se demostraron en 2.2 y 2.3. Por
eso aquí no hace falta inducción: basta con ver qué hace cada operación sobre
el mapa y usar lo que ya está demostrado.

Notación: $c_r(g)$ es el número de sismos de la región $g$ en $r$ y
$\text{mx}_r(g)$ su magnitud máxima:

```math
c_r(g) = |\{ i \mid \text{reg}(s_i) = g \}| \qquad \text{mx}_r(g) = \max \{ \text{mag}(s) \mid s \in r,\ \text{reg}(s) = g \}
```

Si $g \notin \text{regs}(r)$ entonces $c_r(g) = 0$, porque no hay ningún sismo
de $g$.

---

### 3.1 `conteoSeguro`

```scala
def conteoSeguro(r: Registro): Map[String, Int] = {
  conteoPorRegion(r).withDefaultValue(0)
}
```

**Especificación.** Para **cualquier** región $g$, esté o no en el registro:

```math
\forall g : f(r)(g) = c_r(g)
```

**Cómo se evalúa `withDefaultValue`.** Si $M$ es un mapa y $d$ un valor:

```math
M.\text{withDefaultValue}(d)(g) \rightarrow
\begin{cases}
M(g) & \text{si } g \in \text{llaves}(M) \\
d & \text{si } g \notin \text{llaves}(M)
\end{cases}
```

y $\text{llaves}(M.\text{withDefaultValue}(d)) = \text{llaves}(M)$.

**Demostración.** Sea $M = \text{conteoPorRegion}(r)$. Por 2.2,
$\text{llaves}(M) = \text{regs}(r)$ y $M(g) = c_r(g)$ para
$g \in \text{regs}(r)$. Hay dos casos:

- $g \in \text{regs}(r)$: $P_f(r)(g) \rightarrow M(g) = c_r(g)$. ✔
- $g \notin \text{regs}(r)$: $g$ no es llave de $M$, entonces
  $P_f(r)(g) \rightarrow 0$, y como no hay sismos de $g$, $c_r(g) = 0$. ✔

Entonces $\forall r, g : P_f(r)(g) == f(r)(g)$. En el segundo caso no se lanza
excepción porque nunca se busca una llave que no existe sin valor por
defecto.

Con $r = \text{List}()$: $M = \text{Map}()$, ninguna $g$ es llave y todo
responde $0$, que es lo que pide la prueba del registro vacío.

---

### 3.2 `maximaDe`

```scala
def maximaDe(r: Registro, region: String): Option[Double] = {
  magnitudMaxima(r).get(region)
}
```

**Especificación.**

```math
f(r, g) =
\begin{cases}
\text{Some}(\text{mx}_r(g)) & \text{si } g \in \text{regs}(r) \\
\text{None} & \text{si } g \notin \text{regs}(r)
\end{cases}
```

**Cómo se evalúa `get`.**

```math
M.\text{get}(g) \rightarrow
\begin{cases}
\text{Some}(M(g)) & \text{si } g \in \text{llaves}(M) \\
\text{None} & \text{si } g \notin \text{llaves}(M)
\end{cases}
```

**Demostración.** Sea $M = \text{magnitudMaxima}(r)$. Por 2.3,
$\text{llaves}(M) = \text{regs}(r)$ y $M(g) = \text{mx}_r(g)$.

- $g \in \text{regs}(r)$: $P_f(r, g) \rightarrow \text{Some}(M(g)) = \text{Some}(\text{mx}_r(g))$. ✔
- $g \notin \text{regs}(r)$: $P_f(r, g) \rightarrow \text{None}$. ✔

Entonces $\forall r, g : P_f(r, g) == f(r, g)$.

Aquí también es importante lo que se vio en 2.3: `max` solo se llama sobre
grupos no vacíos, entonces calcular $M$ no lanza excepción, y `get` tampoco.

**Ejemplo:** `maximaDe(registro, "Choco")` $\rightarrow$
$M.\text{get}(\text{"Choco"}) \rightarrow \text{Some}(6.0)$, y
`maximaDe(registro, "Amazonas")` $\rightarrow \text{None}$.

---

### 3.3 `regionMasActiva`

```scala
def regionMasActiva(r: Registro): Option[String] = {
  val conteos = conteoPorRegion(r).toList
  val ordenados = conteos.sortBy { case (region, n) => (-n, region) }
  ordenados.headOption.map(_._1)
}
```

**Especificación.** Sea $\text{top}(r) = \max_{g \in \text{regs}(r)} c_r(g)$.

```math
f(r) =
\begin{cases}
\text{None} & \text{si } r = \text{List}() \\
\text{Some}\big(\min_{\text{alf}} \{ g \in \text{regs}(r) \mid c_r(g) = \text{top}(r) \}\big) & \text{si } r \ne \text{List}()
\end{cases}
```

donde $\min_{\text{alf}}$ es la primera en orden alfabético.

**El orden de `sortBy`.** Cada pareja $(g, n)$ se cambia por su criterio
$k(g, n) = (-n, g)$ y las parejas quedan de menor a mayor criterio. Las
tuplas se comparan así:

```math
(a_1, b_1) < (a_2, b_2) \iff a_1 < a_2 \ \lor\ (a_1 = a_2 \land b_1 < b_2)
```

**Cómo se evalúan `headOption` y `map` sobre `Option`.**

```math
\text{List}().\text{headOption} \rightarrow \text{None} \qquad (x :: l).\text{headOption} \rightarrow \text{Some}(x)
```

```math
\text{None}.\text{map}(h) \rightarrow \text{None} \qquad \text{Some}(x).\text{map}(h) \rightarrow \text{Some}(h(x))
```

**Demostración.** Sea $L$ = `conteoPorRegion(r).toList`. Por 2.2, $L$ tiene
exactamente una pareja $(g, c_r(g))$ por cada $g \in \text{regs}(r)$, en algún
orden.

**Caso $r = \text{List}()$:** $\text{regs}(r) = \emptyset$, entonces
$L = \text{List}()$.

```math
P_f(\text{List}()) \rightarrow \text{List}().\text{sortBy}(k).\text{headOption}.\text{map}(\_.\_1) \rightarrow \text{None}.\text{map}(\_.\_1) \rightarrow \text{None}
```

que es $f(\text{List}())$. ✔

**Caso $r \ne \text{List}()$:** hay al menos una región, entonces $L$ no es
vacía y la lista ordenada tampoco. Sea $(g^*, n^*)$ la primera pareja de la
lista ordenada, o sea la de **menor** criterio. Entonces:

```math
P_f(r) \rightarrow \text{Some}((g^*, n^*)).\text{map}(\_.\_1) \rightarrow \text{Some}(g^*)
```

Falta ver que $g^*$ es la región que pide la especificación:

1. El primer componente del criterio es $-n$. El menor $-n$ es el mayor $n$,
   entonces $n^* = \text{top}(r)$: $g^*$ es una de las regiones con más
   sismos.
2. Si hay varias con $n = \text{top}(r)$, todas tienen el mismo primer
   componente $-\text{top}(r)$, y se desempata por el segundo, que es el
   nombre. La menor es la primera en orden alfabético.

Entonces $g^* = \min_{\text{alf}} \{ g \mid c_r(g) = \text{top}(r) \}$ y
$P_f(r) == f(r)$. ✔

**No depende del orden de `toList`.** Las llaves de un mapa son distintas,
entonces no hay dos parejas con el mismo criterio $(-n, g)$. Por eso la lista
ordenada es siempre la misma sin importar en qué orden salieron las parejas
del mapa.

**Ejemplo con empate:** con Zulia y Arauca, un sismo cada una, los criterios
son $(-1, \text{"Zulia"})$ y $(-1, \text{"Arauca"})$. Empatan en $-1$ y
$\text{"Arauca"} < \text{"Zulia"}$, entonces el resultado es
$\text{Some}(\text{"Arauca"})$.

---

## 4. Réplicas perezosas

### 4.1 `replicas`

```scala
def replicas(magnitud: Double): LazyList[Double] = {
  LazyList.iterate(magnitud)(_ * 0.8).map(redondear)
}
```

**Especificación.** Sea $m$ la magnitud inicial. Definimos la secuencia cruda
$a$ y la secuencia de salidas $b$:

```math
a_0 = m, \qquad a_{n+1} = \operatorname{fl}(a_n \cdot 0.8),
\qquad b_n = \text{redondear}(a_n), \qquad n \ge 0
```

$\operatorname{fl}$ representa el resultado de la multiplicación en `Double`;
`redondear` es exactamente el auxiliar dado. En aritmética real, la recurrencia
ideal da $m(0.8)^n$, pero no se supone igualdad exacta con esa expresión en
punto flotante. La salida debe tener el valor $b_n$ en cada posición $n$, sin
un último elemento. La prueba se refiere a cualquier prefijo finito exigido,
no a evaluar toda la secuencia infinita.

**Lema: prefijos de `iterate`.** Si $u = \text{LazyList.iterate}(m)(h)$,
entonces $u_0=m$ y $u_{n+1}=h(u_n)$ para cada posición que se consulta.

- **Caso base:** al exigir el primer elemento, `iterate` entrega su semilla
  $m$. Coincide con $a_0$.
- **Paso inductivo:** supongamos que el elemento crudo en posición $n$ es
  $a_n$. Al exigir el siguiente, `iterate` aplica $h(x)=x\cdot0.8$ en `Double`,
  obteniendo $\operatorname{fl}(a_n\cdot0.8)=a_{n+1}$. Además deja definida
  otra continuación, sin condición de parada.

Por inducción, cada prefijo finito de la secuencia cruda es el especificado.

**Demostración de la salida.** `map(redondear)` conserva las posiciones y
transforma cada elemento crudo $a_n$ en $\text{redondear}(a_n)=b_n$.
Para un prefijo de longitud cero no se exige ningún elemento. Si los primeros
$k$ elementos exigidos son $b_0,\ldots,b_{k-1}$, al exigir uno más el lema
entrega $a_k$ y `map` entrega $b_k$. Por inducción sobre la longitud del
prefijo, toda observación finita coincide con la especificación. ∎

**Por qué se redondea después.** La siguiente entrada de `iterate` es $a_n$,
no $b_n$. Por ejemplo, desde `1.23`, el tercer valor crudo es `0.7872` y su
salida es `0.79`; usar el anterior redondeado `0.98` produciría `0.78`.
La prueba propia del redondeo no acumulado distingue ambos cálculos.

**Orden y evaluación.** El orden es el de la recurrencia: no se pueden
permutar las réplicas sin cambiar el resultado. `take(k)` define un prefijo
perezoso y `toList` exige sus $k$ elementos, sin necesitar el siguiente.
La prueba con una transformación que falla en el cuarto valor comprueba
que exigir tres no exige ese cuarto. `LazyList` memoriza los elementos
evaluados; mantener una referencia a su cabeza puede retener ese prefijo.
Por tanto, esta demostración no afirma un consumo constante de memoria.

### 4.2 `replicasSensibles`

```scala
def replicasSensibles(magnitud: Double, minimo: Double): List[Double] = {
  replicas(magnitud).takeWhile(_ > minimo).toList
}
```

**Especificación.** Con $b$ definido en 4.1 y mínimo $t$, sea $N$ la posición
del primer valor rechazado, cuando existe:

```math
N = \min\{n \ge 0 \mid b_n \le t\}, \qquad
f(m,t)=\text{List}(b_0,\ldots,b_{N-1})
```

En particular, si $N=0$, el resultado es `List()`. Los valores que entran
cumplen $b_i>t$; el testigo de parada cumple $b_N\le t$ y no se incluye.

**Lema: `takeWhile` conserva el prefijo que cumple.** Para el predicado
$p(x)=(x>t)$, se demuestra por inducción sobre el número $N$ de elementos
aceptados antes del primer rechazo:

- **Caso base, $N=0$:** $p(b_0)$ es falso; `takeWhile` produce el prefijo vacío.
  `toList` da `List()`, como pide $f$.
- **Paso inductivo:** si $N=k+1$, $p(b_0)$ es verdadero. `takeWhile` conserva
  $b_0$ y aplica la misma regla a la cola. En ella hay $k$ valores aceptados
  antes del rechazo original $b_{k+1}$. Por HI entrega
  $\text{List}(b_1,\ldots,b_k)$; al anteponer $b_0$ queda exactamente
  $\text{List}(b_0,\ldots,b_k)$.

Por 4.1 la secuencia consultada es $b$, y por el lema su prefijo cumple la
especificación. `toList` materializa ese prefijo sin alterar valores ni orden.
La comparación se aplica a $b_n$, no a $a_n$: `0.79 > 0.788` es verdadero
aunque el valor crudo correspondiente sea `0.7872`. ∎

**Terminación y dominio.** Para magnitudes sísmicas finitas no negativas y
$t\ge0$, existe el testigo $N$. En aritmética real, la decadencia geométrica
alcanza un valor menor que `0.005`, que redondea a cero. En `Double`, mientras
el estado sea al menos `0.005`, la multiplicación por el factor representado
`0.8`, incluido su error de redondeo, lo reduce por un factor menor que `0.81`.
Ese intervalo está lejos del rango subnormal, así que la disminución alcanza
en un número finito de pasos un valor menor que `0.005`; desde allí las salidas
son cero. No es necesario que el estado crudo llegue a cero exacto. Por eso
`takeWhile` encuentra un rechazo incluso cuando $t=0$ y `toList` termina.

Para conocer ese punto, se exige el valor rechazado además de los $N$
aceptados. Si $t<0$, las salidas cero siguen cumpliendo el predicado y no se
garantiza una lista finita. No se extiende esta demostración a entradas
no finitas ni se añade validación que no pide el taller.

---

## 5. Informe combinado

### 5.1 `informe`

```scala
def informe(r: Registro, minimo: Double): Map[String, Resumen] = {
  val conteos = conteoPorRegion(r)
  val maximas = magnitudMaxima(r)
  maximas.map { case (region, maxima) =>
    (region, Resumen(conteos(region), maxima, replicasSensibles(maxima, minimo).length))
  }
}
```

**Especificación.** Para cada región $g$ del registro finito $r$, definimos
$c_g$ como su cantidad de sismos, $M_g$ como su magnitud máxima y $N_g$ como
la longitud del prefijo sensible de esa máxima con mínimo $t$. En el dominio
de terminación de 4.2:

```math
\text{llaves}(f(r,t))=\text{regs}(r), \qquad
\forall g\in\text{regs}(r):
f(r,t)(g)=\text{Resumen}(c_g,M_g,N_g)
```

**Lema: las consultas del conteo están definidas.** Por 2.2 y 2.3,
`conteos` y `maximas` tienen exactamente las llaves $\text{regs}(r)$.
Cada pareja visitada por `maximas.map` tiene, por tanto, su llave en
`conteos`. No se necesita un valor por defecto ni una consulta del punto 3.

**Demostración por composición.** Por los resultados del punto 2,
`conteos(g)` vale $c_g$ y `maximas(g)` vale $M_g$. Por 4.2,
`replicasSensibles(M_g, t)` es exactamente el prefijo sensible; su longitud
vale $N_g$. Así, la transformación de cada pareja produce
$(g,\text{Resumen}(c_g,M_g,N_g))$.

Aplicamos el lema de conservación de llaves del punto 2. La transformación
depende también de la llave para consultar su conteo, pero la mantiene:

- **Caso base:** si el mapa de máximas está vacío, `map` produce `Map()`.
  En particular, para el registro vacío no se consulta ningún conteo ni se
  calcula ninguna réplica.
- **Paso inductivo:** supongamos transformadas correctamente $k$ parejas.
  La siguiente pareja $(g,M_g)$ tiene una llave distinta de las anteriores.
  Por el lema de consultas y 4.2, se agrega exactamente
  $(g,\text{Resumen}(c_g,M_g,N_g))$, sin reemplazar las otras parejas.

Por inducción sobre las parejas del mapa finito, se conservan todas y solo
las regiones originales con su resumen correcto. ∎

**Consecuencias.** Una región no se elimina si $N_g=0$. La máxima $M_g$ se
guarda sin redondear: solo sus réplicas pasan por `redondear`. Si se duplica
un sismo sin cambiar la máxima, aumenta $c_g$ pero no $M_g$ ni $N_g$.

### 5.2 Orden del registro y del mapa

Sea $r_2$ una permutación de $r$ que conserva las repeticiones. Por 2.4,
conserva las regiones, $c_g$ y $M_g$. Como el mismo $M_g$ con el mismo $t$
define la misma recurrencia y el mismo prefijo sensible, también conserva
$N_g$. Entonces:

```math
\text{informe}(r_2,t)==\text{informe}(r,t)
```

La igualdad de mapas compara llaves y valores, no el orden de iteración.
Cada pareja regional se transforma independientemente y conserva su llave;
por ello visitarlas en otro orden produce el mismo mapa. Esto no permite
reordenar las réplicas: dentro de cada prefijo se mantiene el orden de 4.1.
La prueba propia con dos órdenes del registro compara ambos informes con
el mismo mapa esperado explícito.
