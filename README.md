# javamember
check where a java member is used in a class

Text output:

```text
javamember [--exclude <name1,name2>] [--ignore-constants] [--overlapping-clusters] <Java source file>
```

Graphviz DOT output:

```text
javamember --dot <output.dot> [--exclude <name1,name2>] [--ignore-constants] <Java source file>
```

The directed graph contains ellipse-shaped member-variable nodes and box-shaped
method nodes. Edges marked `uses` connect methods to fields they access directly;
edges marked `calls` connect callers to methods invoked in the same class.

Variable names passed to `--exclude` are comma-separated. Whitespace around names
and empty entries are ignored.

`--ignore-constants` excludes fields declared as `static final`.

`--overlapping-clusters` reports maximal groups of pairwise connected variables.
Unlike the default connected-component clustering, a variable may occur in more
than one cluster.
