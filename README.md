# \# Jenkins Shared Library - Docker Pipeline

# 

# \## 📚 Overview

# This Jenkins Shared Library provides reusable pipeline logic for automating Docker application lifecycle across multiple projects.

# 

# \## 🏗️ Structure

# ```

# jenkins-shared-library/

# ├── vars/

# │   └── dockerPipeline.groovy

# └── README.md

# ```

# 

# \## 🚀 Usage

# 

# \### 1. Configure in Jenkins

# 1\. Go to \*\*Manage Jenkins\*\* → \*\*Configure System\*\*

# 2\. Scroll to \*\*Global Pipeline Libraries\*\*

# 3\. Add library:

# &nbsp;  - Name: `jenkins-shared-library`

# &nbsp;  - Default version: `main`

# &nbsp;  - Retrieval method: \*\*Modern SCM\*\* → \*\*Git\*\*

# &nbsp;  - Project Repository: `https://github.com/YOUR\_USERNAME/jenkins-shared-library.git`

# 

# \### 2. Use in Jenkinsfile

# ```groovy

# @Library('jenkins-shared-library') \_

# 

# dockerPipeline(

# &nbsp;   gitUrl: 'https://github.com/YOUR\_USERNAME/your-app.git',

# &nbsp;   imageName: 'your-dockerhub-username/your-app',

# &nbsp;   imageTag: 'v1.0',

# &nbsp;   containerPort: '8080',

# &nbsp;   dockerHubCredentials: 'dockerhub-credentials',

# &nbsp;   branch: 'main'

# )

# ```

# 

# \## ⚙️ Parameters

# 

# | Parameter | Required | Default | Description |

# |-----------|----------|---------|-------------|

# | `gitUrl` | ✅ Yes | - | Git repository URL |

# | `imageName` | ✅ Yes | - | Docker image name (username/image) |

# | `imageTag` | ❌ No | `latest` | Docker image tag |

# | `containerPort` | ❌ No | `8080` | Container port mapping |

# | `dockerHubCredentials` | ❌ No | `dockerhub-credentials` | Jenkins credential ID |

# | `dockerfilePath` | ❌ No | `Dockerfile` | Path to Dockerfile |

# | `appDirectory` | ❌ No | `.` | Application directory |

# | `branch` | ❌ No | `main` | Git branch to clone |

# 

# \## 📦 Pipeline Stages

# 

# 1\. \*\*Clone Repository\*\* - Clones source code from Git

# 2\. \*\*Build Docker Image\*\* - Builds Docker image from Dockerfile

# 3\. \*\*Run Docker Container\*\* - Starts container for testing

# 4\. \*\*Test Container\*\* - Validates container health

# 5\. \*\*Push to Docker Hub\*\* - Pushes image to Docker Hub

# 6\. \*\*Cleanup\*\* - Removes local test container

# 

# \## 🔐 Prerequisites

# 

# \- Jenkins with Docker installed

# \- Docker Hub credentials configured in Jenkins

# \- Git access to repositories

