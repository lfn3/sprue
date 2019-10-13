# sprue

Repl driven tool for generating models. 

### TODO

- ~~Compat tests~~
~~- Fix dodgy `base-class` handling~~
  ~~- No `extends` keyword~~
  ~~- Fields and getters still showing up in subclass~~
- How does it handle generic fields? e.g. `Collection<Long>`?
  - It does not. Probably need to extend the `::base/type` and `convert-type` bits?
    Not clear how to handle that when it comes to table generation?
    Maybe there's an idea of embedding or referencing another entity, or a collection of entities?
    That would determine we just use an Id or instead include the whole object?
- Implement interfaces
- Move repl required defs off to config file somewhere? Maybe split out ns
- More specific specs for flags
- Postgres schema generator
- Some way of producing referential or cut down entities 
  (think create/update case)
- Generate Jooq mappers
- Wiring from API end?

## License

Copyright © 2019 Liam Falconer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
