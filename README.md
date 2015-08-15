**Adds an export context menu to FindBugs Bugs in Eclipse to discuss bugs on social coding platforms.**

This eclipse plugin extends the [FindBugs](http://findbugs.sourceforge.net/) eclipse plugin. It uses the web form(s) to create issues; it currently does not rely on [Mylyn](http://www.eclipse.org/mylyn/) at the moment. I dont know if this is a feature or a bug :P.

## Limitations

Currently this plugin is not tested throughly and supports only GitHub partially.

## Motivation

Many developers can't decide on their own if static analysis bugs are relevant and how to fix them correctly. This is caused by non-intuitive bug descriptions and possible false positives.
This plugin trys to provide better interaction through exporting the found bug to the discussion platform used by the project.

## Installation

*TODO*

## Dependencies

- FindBugs
- JGit

## Tests

*TODO* (currently there are now tests, PRs welcome)

## License

This plugin is licensed under a [MIT License](LICENSE)
