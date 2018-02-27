# JiraFirebase

## Functionality

This code reads issues from JIRA, then stores them in FireBase. It then checks the last stored version, and compares it to the newly added version. Any differences are noted, then sent as a mass email. The last stored version is deleted, and the new version replaces it.
This code is designed to be run every few minutes.

## Backstory

This was created as part of an internship I did with a game development company over the summer of 2017. The company I was working with used SVN for version control. As such, this repo doesn't have many commits outside of some setup to put it on GitHub. This is not because I wasn't using version control, but because I wasn't using GitHub for it.
This is also why I don't have a .gitignore, since I did all that cleanup with SVN.

## Downloading

If you were to download the code now, it would not run. Why? Because the functional version contains sensitive information for both the company I was interning at and its employees (see [the backstory of this repo](#backstory)). So I do not recommend downloading the code to use it. However, if you are looking to do something similar, feel free to look through the code and [shoot me an email](mailto:hazel.r.pearson@gmail.com) if you have any questions!

## Licensing

This code is licensed under the GNU General Public License v3.0. To summarize, the license...

GNU GPLv3 means you can:
- Use my code commercially
- Distribute it
- Modify it
- Patent it
- Use it privately

But have to:
- Disclose its source (here!)
- Provide the [copyright notice](#legalese) and license, which appears in my LICENSE.md
- License your code under this same license (GNU GPLv3)
- Document what changes you make to my code

And:
- You don't have a warranty
- I'm not liable

Copyright (C) 2017 Hazel Pearson

### Legalese

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see [http://www.gnu.org/licenses/](http://www.gnu.org/licenses/).

You can contact me with any questions about licensing [here](mailto:hazel.r.pearson@gmail.com).