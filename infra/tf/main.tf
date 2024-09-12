# ------------- ECS Cluster -------------
resource "aws_ecs_cluster" "main" {
  name = "my-ecs-cluster"
}

# ------------- ECR Repository -------------
resource "aws_ecr_repository" "main" {
  name                 = "my-ecr-repo"
  image_tag_mutability = "MUTABLE"
}

# ------------- ECS Task Definition (Expose Port 8080) -------------
resource "aws_ecs_task_definition" "main" {
  family                   = "my-ecs-task"
  network_mode             = "awsvpc"
  container_definitions    = jsonencode([
    {
      name        = "my-container"
      image       = "${aws_ecr_repository.main.repository_url}:latest"
      cpu         = 256
      memory      = 512
      essential   = true
      portMappings = [
        {
          containerPort = 8080  # Update to 8080
          hostPort      = 8080  # Update to 8080
        }
      ]
      logConfiguration = {
          logDriver = "awslogs"
          options = {
            "awslogs-group"         = "/ecs/my-ecs-task"
            "awslogs-region"        = "eu-central-1"
            "awslogs-create-group"  = "true"
            "awslogs-stream-prefix" = "ecs"
            "mode"                  = "non-blocking"
            "max-buffer-size"       = "25m"
          }
      }
    }
  ])
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  cpu                      = "256"
  memory                   = "512"
}

# ------------- ECS Service -------------
resource "aws_ecs_service" "main" {
  name            = "my-ecs-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.main.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = local.subnet_ids
    assign_public_ip = true
    security_groups = [aws_security_group.ecs_service_sg.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.ecs_tg.arn
    container_name   = "my-container"
    container_port   = 8080  # Update to 8080
  }

  depends_on = [aws_lb_listener.ecs_listener]
}

# ------------- ALB -------------
resource "aws_lb" "ecs_alb" {
  name               = "ecs-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.ecs_service_sg.id]
  subnets            = local.subnet_ids  # Use local.subnet_ids
}

# ------------- ALB Target Group -------------
resource "aws_lb_target_group" "ecs_tg" {
  name        = "ecs-tg"
  port        = 8080              # Update to 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"
    # Health check configuration
    health_check {
      path                = "/actuator/health"  # Health check path
      interval            = 30                  # Time between health checks
      timeout             = 5                   # Health check timeout
      healthy_threshold   = 2                   # Number of successful checks to mark healthy
      unhealthy_threshold = 2                   # Number of failed checks to mark unhealthy
      matcher             = "200-299"           # HTTP status codes for healthy response
    }
}

# ------------- ALB Listener (Keep HTTP Listener on port 80) -------------
resource "aws_lb_listener" "ecs_listener" {
  load_balancer_arn = aws_lb.ecs_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ecs_tg.arn
  }
}

# ------------- API Gateway -------------
resource "aws_api_gateway_rest_api" "api" {
  name        = "my-api"
  description = "My ECS API Gateway"
}

resource "aws_api_gateway_resource" "resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "{proxy+}"  # Proxy all requests and paths
}

resource "aws_api_gateway_method" "method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.resource.id
  http_method   = "ANY"  # Allow all HTTP methods
  authorization = "NONE"

  request_parameters = {
    "method.request.path.proxy" = true  # Enable proxy path forwarding
  }
}

resource "aws_api_gateway_integration" "alb_integration" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.resource.id
  http_method = aws_api_gateway_method.method.http_method
  type        = "HTTP_PROXY"  # Proxy all requests to ALB

  # ALB DNS Name used as the URI
  uri         = "http://${aws_lb.ecs_alb.dns_name}/{proxy}"  # Forward all paths to ALB

  integration_http_method = "ANY"
  passthrough_behavior    = "WHEN_NO_MATCH"
  request_parameters = {
    "integration.request.path.proxy" = "method.request.path.proxy"
  }
}


resource "aws_api_gateway_deployment" "deployment" {
  depends_on = [aws_api_gateway_integration.alb_integration]
  rest_api_id = aws_api_gateway_rest_api.api.id
  stage_name  = "dev"
}

# ------------- Security Group for ECS Service (Allow Port 80 and 8080) -------------
resource "aws_security_group" "ecs_service_sg" {
  vpc_id = data.aws_vpc.default.id

  # Allow HTTP traffic to the ALB on port 80
  ingress {
    from_port   = 80  # Allow traffic to port 80
    to_port     = 80  # Allow traffic to port 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # Open to the internet
  }

  # Allow traffic to the ECS service on port 8080
  ingress {
    from_port   = 8080  # Allow traffic to port 8080
    to_port     = 8080  # Allow traffic to port 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # Open to the internet (can be more restricted)
  }

  # Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}


# ------------- IAM Role for ECS Task Execution -------------
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "ecs-task-execution-role"

  assume_role_policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Effect": "Allow",
        "Principal": {
          "Service": "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecr_access_policy" {
  name = "ecs-ecr-access-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability"
        ],
        "Resource": "*"
      }
    ]
  })
}
