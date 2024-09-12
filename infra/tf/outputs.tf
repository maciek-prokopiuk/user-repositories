output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecr_repository_url" {
  value = aws_ecr_repository.main.repository_url
}

output "api_gateway_url" {
  value = "${aws_api_gateway_deployment.deployment.invoke_url}/v1"
}

output "alb_url" {
  value = aws_lb.ecs_alb.dns_name
}
