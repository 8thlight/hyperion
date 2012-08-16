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
  task :spec => :build do
    spec(name)
  end
end

namespace :core do
  package('core')
end

namespace :sql do
  task :build => 'core:install'
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

namespace :sqlite do
  task :build => ['core:install', 'sql:install']
  package('sqlite')
end

namespace :gae do
  task :build => ['core:install']
  package('gae')
end

namespace :riak do
  task :build => ['core:install']
  package('riak')
end

PROJECTS = [:core, :sql, :postgres, :mysql, :sqlite, :gae, :riak]

def create_task_for_all(task_name)
  task task_name => PROJECTS.map {|project| "#{project}:#{task_name}"}
end

desc 'Run the specs Hyperion'
create_task_for_all(:spec)

desc 'Deploy Hyperion'
create_task_for_all(:deploy)

desc 'Clean Hyperion'
create_task_for_all(:clean)

desc 'Install Hyperion'
create_task_for_all(:install)

def clean(dir)
  lein_task(dir, 'clean')
  rm_script = 'rm -rf lib classes pom.xml .lein-deps-sum'
  sh "cd #{dir} && #{rm_script}"
  sh rm_script
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
  lein_task(dir, 'push')
end

def lein_task(dir, task)
  sh "cd #{dir} && lein #{task}"
end

task :default => :spec
