---
title: Environments
---

# Environments

Multiple environments can be created for each subproject, for example, one for Production, one for Staging, one for
Development.

Each environment may contain the followings:
- Zero or more variables
- HTTP protocol version preference
- SSL verification preference

## Creating an Environment

![Creating an Environment](../create-environment.gif)

To create one, click the pencil next to the "Environment" label to open the Environment dialog, then click the "+" 
button.

## Environment Variables

![Using Variables](../using-environment-variable.gif)

Variables defined in the environment can be used anywhere in Requests. Just type `${{variable-name}}` in a Request to
use.

Variables that can be resolved are highlighted in blue, otherwise it is in red.

If no environment is selected, all variables are unresolvable and thus they will be all red.
