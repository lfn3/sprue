# sprue

Repl driven tool for generating models. 

### TODO

- ~~Compat tests~~
- ~~Fix dodgy `base-class` handling~~
  - ~~No `extends` keyword~~
  - ~~Fields and getters still showing up in subclass~~
- How does it handle generic fields? e.g. `Collection<Long>`?
  - It does not. Probably need to extend the `::base/type` and `convert-type` bits?
    Not clear how to handle that when it comes to table generation?
    Maybe there's an idea of embedding or referencing another entity, or a collection of entities?
    That would determine we just use an Id or instead include the whole object?
    Also doesn't handle generic interfaces. 
    JavaPoet has `ParameterizedTypeName` which will probably be what should be used for the implementation,
    and I think I know how I want to write that, even.
- ~~Implement interfaces~~
- ~~Package includes type name~~
- ~~For some reason the equals method has `public` on the parameter?~~
- Not importing `Objects`
- ~~Don't need `Object.toString` in `toString` method~~
- `@Nullable` not on getter or ctor param
- ~~Some of the strings we emit could be characters (mostly `"}"`)~~
- Don't need classname qualifier on use of const field.
- Should be `isInEffect` not `getInEffect`
- Flag for suppressing `toString` when we want it to come from the base class
- Postgres schema/DDL generator
- Fix up equals methods
- Move repl required defs off to config file somewhere? Maybe split out ns
- More specific specs for flags
- Some way of producing referential or cut down entities 
  (think create/update case)
- Generate Jooq mappers
- Wiring from API end?
- Check fields mentioned in `base-type` are in the field definition?
- Builder generator?
  - Probably want a `default` flag on fields for this case (which could also emit something into the swagger annotation?)

## License

Copyright © 2019 Liam Falconer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
