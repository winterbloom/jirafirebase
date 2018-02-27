# JiraFirebase

## Functionality

This code reads issues from JIRA, then stores them in FireBase. It then checks the last stored version, and compares it to the newly added version. Any differences are noted, then sent as a mass email. The last stored version is deleted, and the new version replaces it.
This code is designed to be run every few minutes.

## Backstory

This was created as part of an internship I did with a game development company over the summer of 2017. The company I was working with used SVN for version control. As such, this repo doesn't have many commits outside of some setup to put it on GitHub. This is not because I wasn't using version control, but because I wasn't using GitHub for it.
This is also why I don't have a .gitignore, since I did all that cleanup with SVN.

## Downloading

If you were to download the code now, it would not run. Why? Because the functional version contains sensitive information for both the company I was interning at and its employees (see [the backstory of this repo](#backstory)). So I do not recommend downloading the code to use it. However, if you are looking to do something similar, feel free to look through the code and [shoot me an email](mailto:hazel.r.pearson@gmail.com) if you have any questions!