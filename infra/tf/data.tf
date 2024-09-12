# Get default VPC
data "aws_vpc" "default" {
  default = true
}

# Get default Subnets in the VPC
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Use the subnet IDs directly
locals {
  subnet_ids = data.aws_subnets.default.ids
}
