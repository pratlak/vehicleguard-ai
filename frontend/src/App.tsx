import React, { useState } from 'react';
import { Layout, Typography, Space } from 'antd';
import { SafetyOutlined } from '@ant-design/icons';
import QuoteForm from './pages/QuoteForm';
import QuoteResult from './pages/QuoteResult';
import ChatPanel from './components/ChatPanel';
import { AssessResponse } from './types';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;

const App: React.FC = () => {
  const [quoteResult, setQuoteResult] = useState<AssessResponse | null>(null);

  const handleQuoteResult = (result: AssessResponse) => {
    setQuoteResult(result);
  };

  const handleReset = () => {
    setQuoteResult(null);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#001529', padding: '0 24px' }}>
        <Space align="center">
          <SafetyOutlined style={{ fontSize: 24, color: '#1677ff' }} />
          <Title level={4} style={{ color: 'white', margin: 0 }}>
            VehicleGuard AI
          </Title>
        </Space>
        <span style={{ color: '#8c8c8c', marginLeft: 16, fontSize: 14 }}>
          Intelligent Vehicle Insurance Risk Assessment
        </span>
      </Header>

      <Content style={{ padding: '24px', background: '#f5f5f5' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          {quoteResult ? (
            <QuoteResult result={quoteResult} onReset={handleReset} />
          ) : (
            <QuoteForm onResult={handleQuoteResult} />
          )}
        </div>
      </Content>

      <ChatPanel quoteResult={quoteResult} onQuoteResult={handleQuoteResult} />

      <Footer style={{ textAlign: 'center', background: '#001529', color: '#8c8c8c' }}>
        VehicleGuard AI © {new Date().getFullYear()} — Powered by Claude AI
      </Footer>
    </Layout>
  );
};

export default App;
