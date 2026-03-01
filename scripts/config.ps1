# FloraFauna repo-specific configuration for worktree scripts
# All paths are relative to repo root unless specified

$script:SharedScriptsDir = "C:\code\scripts\worktree"

# Dev world and options templates
$script:WorldTemplatePath = "dev\world-template"
$script:OptionsTemplatePath = "dev\options-template.txt"

# Run directory pattern ({0} = worktree/branch name)
$script:RunDirPattern = "run\client-{0}"

# Gradle tasks
$script:DataGenTask = "runData"
$script:ClientTask = "runClient"
