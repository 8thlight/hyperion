def package(name)
  desc "Clean #{name}"
  task :clean do
    clean(name)
  end

  desc "Gather dependencies for #{name}"
  task :deps do
    deps(name)
  end

  desc "Build #{name}"
  task :build => [:clean, :deps]

  desc "Install #{name}"
  task :install => :build do
    _install(name)
  end

  desc "Deploy #{name}"
  task :deploy => :build do
    deploy(name)
  end

  desc "Run #{name} specs"
  task :specs => :build do
    spec(name)
  end
end

namespace :core do
  package('core')
end

namespace :sql do
  package('sql')
end

namespace :postgres do
  task :build => ['core:install', 'sql:install']
  package('postgres')
end

namespace :mysql do
  task :build => ['core:install', 'sql:install']
  package('mysql')
end

namespace :gae do
  task :build => 'core:install'
  package('gae')
end

desc 'Run specs for all Hyperion Datastores'
task :specs => ['core:specs', 'sql:specs', 'postgres:specs', 'mysql:specs', 'gae:specs']

desc 'Deploy Hyperion'
task :deploy => ['core:deploy', 'sql:deploy', 'postgres:deploy', 'mysql:deploy', 'gae:deploy']

def clean(dir)
  lein_task(dir, 'clean')
  sh "cd #{dir} && rm -rf lib classes pom.xml .lein-deps-sum"
end

def deps(dir)
  lein_task(dir, 'deps')
end

def spec(dir)
  lein_task(dir, 'spec')
end

def _install(dir)
  lein_task(dir, 'install')
end

def deploy(dir)
  lein_task(dir, 'deploy')
end

def lein_task(dir, task)
  sh "cd #{dir} && lein #{task}"
end
