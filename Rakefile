def package(name, dependencies)
  desc "Clean #{name}"
  task :clean do
    clean(name)
  end

  desc "Gather dependencies for #{name}"
  task :deps => [:checkouts] do
    deps(name)
  end

  desc "Build #{name}"
  task :build => [:checkouts]

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

  desc "Build #{name} pom.xml"
  task :pom do
    pom(name)
  end

  desc "Setup checkouts for #{name}"
  task :checkouts do
    checkouts(name, dependencies)
  end
end

namespace :api do
  package('api', [])
end

namespace :sql do
  #task :build => 'api:install'
  package('sql', %w{api})
end

namespace :postgres do
  #task :build => ['api:install', 'sql:install']
  package('postgres', %w{api sql})
end

namespace :mysql do
  #task :build => ['api:install', 'sql:install']
  package('mysql', %w{api sql})
end

namespace :sqlite do
  #task :build => ['api:install', 'sql:install']
  package('sqlite', %w{api sql})
end

namespace :gae do
  #task :build => ['api:install']
  package('gae', %w{api})
end

namespace :riak do
  #task :build => ['api:install']
  package('riak', %w{api})
end

namespace :mongo do
  #task :build => ['api:install']
  package('mongo', %w{api})
end

namespace :redis do
  #task :build => ['api:install']
  package('redis', %w{api})
end

CI_PROJECTS = [:api, :sql, :postgres, :mysql, :sqlite, :gae, :redis, :mongo]
PROJECTS = CI_PROJECTS + [:riak]

def create_task_for_all(task_name)
  task task_name => PROJECTS.map {|project| "#{project}:#{task_name}"}
end

def create_task_for_ci(task_name, sub_task)
  task task_name => CI_PROJECTS.map {|project| "#{project}:#{sub_task}"}
end

desc 'Setup checkouts for subprojets'
create_task_for_all(:checkouts)

desc 'Run the specs Hyperion'
create_task_for_all(:spec)

desc 'Run the ci specs Hyperion'
create_task_for_ci(:ci_spec, :spec)

desc 'Build pom files'
create_task_for_all(:pom)

desc 'Deploy Hyperion'
create_task_for_all(:deploy)

desc 'Clean Hyperion'
create_task_for_all(:clean)

desc 'Install Hyperion'
create_task_for_all(:install)

def clean(dir)
  lein_task(dir, 'clean')
  rm_script = 'rm -rf lib target pom.xml .lein-deps-sum'
  sh "cd #{dir} && #{rm_script}"
  sh rm_script
end

def deps(dir)
  lein_task(dir, 'deps')
end

def spec(dir)
  lein_task(dir, 'spec')
end

def pom(dir)
  lein_task(dir, 'pom')
end

def _install(dir)
  lein_task(dir, 'install')
end

def deploy(dir)
  lein_task(dir, 'push')
end

def lein_bin
  ENV['CI'] ? 'lein2' : 'lein'
end

def lein_task(dir, task)
  sh "cd #{dir} && #{lein_bin} #{task}"
end

def checkouts(client, servers)
  Dir.mkdir "#{client}/checkouts" unless File.exists?("#{client}/checkouts")
  servers.each do |server|
    ln_path = "#{client}/checkouts/#{server}"
    if !(File.exists?(ln_path))
      sh "ln -s #{File.expand_path(File.dirname(__FILE__))}/#{server} #{client}/checkouts/#{server}"
    end
  end
end

task :default => :spec
