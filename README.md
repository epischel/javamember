# javamember
check where a java member is used in a class

Text output:

```text
javamember [--exclude <name1,name2>] <Java source file>
```

Graphviz DOT output:

```text
javamember --dot <output.dot> [--exclude <name1,name2>] <Java source file>
```

Variable names passed to `--exclude` are comma-separated. Whitespace around names
and empty entries are ignored.
