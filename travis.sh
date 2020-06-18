#!/usr/bin/env bash
set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v58 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
  source ~/.local/bin/setup_promote_environment
}
configureTravis

. regular_gradle_build_deploy_analyze
promote