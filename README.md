Across Variants Query Plugin
============================

A decompounding-variants aware, across fields, conjunctive query.

The Across Variants Query plugin provides with a new query and filter type that permits querying multiple variants tokens for same position (like different compound analysis) and supports querying each variant across multiple fields.

Installation
------------

Simply run at the root of your ElasticSearch installation:

	bin/plugin --install com.yakaz.elasticsearch.plugins/elasticsearch-query-acrossvariants/1.4.1

This will download the plugin from the Central Maven Repository.

In order to declare this plugin as a dependency, add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.yakaz.elasticsearch.plugins</groupId>
    <artifactId>elasticsearch-query-acrossvariants</artifactId>
    <version>1.4.1</version>
</dependency>
```

Version matrix:

	┌──────────────────────────────┬─────────────────────────┐
	│ Across Variants Query Plugin │ ElasticSearch           │
	├──────────────────────────────┼─────────────────────────┤
	│ 1.4.x                        │ 1.0.0.RC1 ─► 1.2.4      │
	├──────────────────────────────┼─────────────────────────┤
	│ 1.3.x                        │ 0.90.8 ─► (0.90.11)     │
	├──────────────────────────────┼─────────────────────────┤
	│ 1.2.x                        │ 0.90.6, 0.90.7,         │
	│                              │ 1.0.0.Beta1             │
	├──────────────────────────────┼─────────────────────────┤
	│ 1.1.x                        │ 0.90 ─► 0.90.5          │
	├──────────────────────────────┼─────────────────────────┤
	│ 1.0.x                        │ 0.19 ─► 0.20.0          │
	└──────────────────────────────┴─────────────────────────┘

Description
-----------

Compound words are sometimes written glued, sometimes with dashes and sometimes separated, although this mostly depends on the used language.

For some dash-separated words, if dashes are missing, finding the compounded words is difficult.
Hence it is often easier to index the compounds and the glued word, querying whatever the user entered as a query.
But we can do better by querying both analysis (separated and glued compounds) and calling a match any document matching any variant.
However it becomes difficult to merge this disjunction with conjunctive queries.

For example: `wi-fi AND monitoring` should match either variant of `"wi-fi"` and `"monitoring"`. Hence it should become `(wifi OR (wi AND fi)) AND monitoring`.<br/>
This plugin relies on the analysis to tokenize `"wi-fi monitoring"` into `wi`, `fi` and `wifi` at position `0` and `monitoring` at position `1`.

This plugin permits querying each term across multiple fields so that querying `"wi-fi monitoring"`, `"wifi monitoring"` or `"wi fi monitoring"` against `name` and `category` should match the document `{name:"WiFi analyzer", category:"monitoring tools"}`.

Configuration
-------------

The plugin provides you with the `across_variants` query and filter.<br/>
It expects a list of fields to be queried, under the `fields` property, the `value` to query, an optional `analyzer` to tokenize it.

Additionally you can customize the leaf query type that is used in the `BooleanQuery` tree, it defaults to a simple `TermQuery`.
Using such customization, you can decide to use a `PrefixQuery` instead, or even change the field the query will run against.

### Reference

```js
across_variants: {
    // Mandatory: Fields specification, multiple variants are possible
    fields: "field",
    fields: "boosted_field^2",
    fields: "first_field,second_boosted_field^2",
    fields: ["first_field", "second_boosted_field^2"],
    fields: {
        first_field: 1.0,
        second_boosted_field: 2.0
    }
    // Mandatory: Value specification
    value: "queried text",
    text:  "queried text",
    // Optional: whether to use a `dis max` query
    use_dis_max: false, // default value
    tie_breaker: 0.0,   // default value
    // Optional: Analyzer for value specification
    analyzer: "default_search" // the default search analyzer is used by default
    // Optional: Query type customization
    script: "ctx.query = new org.apache.lucene.search.TermQuery(ctx.term)", // script equivalent of the default behavior
    lang: "mvel", // ElasticSearch default script language
    params: {} // any custom parameters for the script
}
```

The `across_variants` query accepts a `boost` parameter, the `across_variants` _filter_ naturally does not.

The query type customization feature through scripting requires some more details.<br/>
You should work with the given context `ctx` variable. Here is a description of its content:

<blockquote><dl>
    <dt><code>ctx.text</code></dt>
    <dd>The queried term's text.</dd>
    <dt><code>ctx.field</code></dt>
    <dd>The queried term's field.</dd>
    <dt><code>ctx.term</code></dt>
    <dd>A <code>org.apache.lucene.index.Term</code> configured with the previously described <code>ctx.field</code> and <code>ctx.text</code>.</dd>
    <dt><code>ctx.query</code> or <code>ctx.filter</code></dt>
    <dd>The query/filter to be used to query the given text against the given field.
      This is the script's output.</dd>
</dl></blockquote>

### Example

Let's assume the following analysis settings and create a simple index and mapping:

```sh
curl -XPUT 'localhost:9200/test' -d '
index:
    analysis:
        analyzer:
            compound_variants:
                type: custom
                tokenizer: whitespace
                filter:
                - decompounder
                - lowercase
        filter:
            decompounder:
                type: word_delimiter_2
                split_on_case_change: true
                catenate_all: true
                preserve_original: true
                # The following is very important!
                all_parts_at_same_position: true'
curl -XPUT 'localhost:9200/test/test/_mapping' -d '
{
    test: {
        properties: {
            name:     { type: "string", analyzer: "compound_variants" },
            category: { type: "string", analyzer: "compound_variants" }
        }
    }
}'
```

You should get the following output while analyzing `"wi-fi monitoring"`:

```sh
curl -XGET 'localhost:9200/test/_analyze?analyzer=compound_variants&pretty=true' -d 'wi-fi monitoring'
```
```json
{
  "tokens" : [ {
    "token" : "wi-fi",
    "start_offset" : 0,
    "end_offset" : 5,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "wi",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "fi",
    "start_offset" : 3,
    "end_offset" : 5,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "wifi",
    "start_offset" : 0,
    "end_offset" : 5,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "monitoring",
    "start_offset" : 6,
    "end_offset" : 16,
    "type" : "word",
    "position" : 2
  } ]
}
```

This shows that `"wi-fi"` gets analyzed into _either_: `wi-fi`, or `wi` _and_ `fi`, or `wifi`.

Let's create some documents:

```sh
curl -XPUT 'localhost:9200/test/test/glued'  -d '{name: "wifi  analyzer", category: "monitoring tools"}'
curl -XPUT 'localhost:9200/test/test/dashed' -d '{name: "wi-fi analyzer", category: "monitoring tools"}'
curl -XPUT 'localhost:9200/test/test/cased'  -d '{name: "WiFi  analyzer", category: "monitoring tools"}'
curl -XPUT 'localhost:9200/test/test/spaced' -d '{name: "wi fi analyzer", category: "monitoring tools"}'
curl -XPOST 'localhost:9200/_refresh'
```

Now let's test some queries:

```sh
curl -XGET 'localhost:9200/test/test/_search?pretty=true' -d '
{
    query: {
        across_variants: {
            fields: ["name^2","category"],
            value: "wi-fi monitoring",
            analyzer:"compound_variants"
        }
    }
}'
```

Here is a table explaining what document will match what query variant:

	        ╲ variant │ "wifi"  │ "wi─fi" │ "WiFi"  │ "wi fi"
	document ╲        │         │         │         │
	──────────────────┼─────────┼─────────┼─────────┼─────────
	glued             │    ✓    │    ✓    │    ✓    │    ✘
	dashed            │    ✓    │    ✓    │    ✓    │    ✓
	cased             │    ✓    │    ✓    │    ✓    │    ✓
	spaced            │    ✘    │    ✓    │    ✓    │    ✓

`"wi fi"` doesn't match `glued`  because `"wi fi"` analyzes into `wi` and `fi`, and `glued`  only has term  `wifi`.<br/>
`"wifi"`  doesn't match `spaced` because `"wifi"`  analyzes into `wifi`,        and `spaced` only has terms `wi` and `fi`.

Behind the scene
----------------

The plugin constructs nested `BooleanQuery`s out of the analysis of the given value.
It uses the `position`, `start_offset` and `end_offset` to know how the tokens should be assembled.
The plugin will _and_ together every subquery it constructs for each position.
Inside each position it adds the longest tokens, then tries to nest smaller tokens into the longer ones.
If two tokens have the same start and end offsets, they become alternatives.

Hence with the analysis details given above in the example section, the constructed tree will be:

	            ╭────────╮
	            │ (root) │
	            ╰───┬────╯
	        ┌───────┴────────┐
	    ╭───┴────╮           │
	    │  wifi  │     ╭─────┴──────╮
	    ├───OR───┤ AND │ monitoring │
	    │ wi-fi  │     ╰────────────╯
	    ╰───┬────╯
	       OR
	  ┌─────┴────┐
	╭─┴──╮     ╭─┴──╮
	│ wi │ AND │ fi │
	╰────╯     ╰────╯

And the constructed query will look like: `( wifi OR wi-fi OR (wi AND fi) ) AND monitoring`.

Moreover, if you specify multiple fields, each term gets queried across those fields, for example: `wifi` is actually `(name:wifi OR category:wifi)`.
And boosting is applied in a per field manner.

See also
--------

https://github.com/yakaz/elasticsearch-analysis-combo/

https://github.com/yakaz/elasticsearch-analysis-worddelimiter2/
