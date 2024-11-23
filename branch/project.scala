//> using scala "3.5.2"
//> using jvm 23

//> using  options -no-indent -rewrite -source:3.4-migration

//> using publish.name branch
//> using publish.organization dev.wishingtree
//> using publish.url https://github.com/wishingtreedev/branch
//> using publish.vcs github:wishingtreedev/branch
//> using publish.license Apache-2.0
//> using publish.repository sonatype:public
//> using publish.developer "alterationx10|Mark Rudolph|https://alterationx10.com"

//> using repository sonatype:public

//> using test.dep org.scalameta::munit:1.0.2
//> using test.dep org.testcontainers:testcontainers:1.20.4
//> using test.dep com.h2database:h2:2.3.232
//> using test.dep org.postgresql:postgresql:42.7.4
//> using test.resourceDir ../test_resources

//> using publish.version 0.0.1-SNAPSHOT
