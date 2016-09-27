### Kleisli

Kleisli is a function composition for functions which map a sim0ple value `a` to a monadic value `m a`
 
orig: `a -> m b`, `b -> m c`
kleisli: `a -> b`, `b -> c`

e.g. the `Writer` monad - `a -> (string, b)`
 
Being monadic is not always necessary - being a functor often suffices. At least in the implementation terms of `cats`

haskell & scalaz offer the `>=>` composition (aka the fish op): `(f >=> g) x = f x >>= g`

from cats docs: 

Kleisli can be viewed as the monad transformer for functions. Recall that at its essence, Kleisli[F, A, B] is just a function A => F[B], with niceties to make working with the value we actually care about, the B, easy. Kleisli allows us to take the effects of functions and have them play nice with the effects of any other F[_].

