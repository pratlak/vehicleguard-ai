import React from 'react';
import { Card, Table, Tag, Typography, Progress, Space } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { AppliedFactor } from '../types';

const { Text } = Typography;

interface RiskBreakdownCardProps {
  factors: AppliedFactor[];
}

const RiskBreakdownCard: React.FC<RiskBreakdownCardProps> = ({ factors }) => {
  const maxImpact = Math.max(...factors.map(f => f.scoreImpact), 1);

  const CATEGORY_COLOR: Record<string, string> = {
    driver: 'blue',
    vehicle: 'purple',
    coverage: 'cyan',
    location: 'green',
  };

  const getCategoryFromKey = (key: string): string => {
    if (key.startsWith('driver') || key.startsWith('license') ||
        key.startsWith('violation') || key.startsWith('accident')) return 'driver';
    if (key.startsWith('vehicle')) return 'vehicle';
    if (key.startsWith('coverage')) return 'coverage';
    if (key.startsWith('high_density')) return 'location';
    return 'other';
  };

  const columns = [
    {
      title: 'Risk Factor',
      dataIndex: 'label',
      key: 'label',
      render: (label: string, record: AppliedFactor) => (
        <Space direction="vertical" size={0}>
          <Text strong>{label}</Text>
          <Tag color={CATEGORY_COLOR[getCategoryFromKey(record.key)] || 'default'} style={{ fontSize: 11 }}>
            {getCategoryFromKey(record.key).toUpperCase()}
          </Tag>
        </Space>
      ),
    },
    {
      title: 'Score Impact',
      dataIndex: 'scoreImpact',
      key: 'scoreImpact',
      width: 200,
      render: (impact: number) => (
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          <Text>{impact.toFixed(1)} pts</Text>
          <Progress
            percent={(impact / maxImpact) * 100}
            size="small"
            showInfo={false}
            strokeColor={impact >= 15 ? '#f5222d' : impact >= 10 ? '#fa8c16' : '#faad14'}
          />
        </Space>
      ),
    },
    {
      title: 'Premium Multiplier',
      dataIndex: 'multiplier',
      key: 'multiplier',
      width: 160,
      render: (mult: number) => (
        <Tag color={mult >= 1.3 ? 'red' : mult >= 1.2 ? 'orange' : 'gold'}>
          ×{mult.toFixed(3)}
        </Tag>
      ),
    },
  ];

  return (
    <Card
      title={
        <Space>
          <WarningOutlined style={{ color: '#fa8c16' }} />
          <span>Risk Factors Applied ({factors.length})</span>
        </Space>
      }
    >
      <Table
        dataSource={factors}
        columns={columns}
        rowKey="key"
        pagination={false}
        size="middle"
        locale={{ emptyText: 'No risk factors applied' }}
      />
    </Card>
  );
};

export default RiskBreakdownCard;
